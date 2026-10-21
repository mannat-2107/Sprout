package backend;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

/**
 * Postgres-backed order creation.
 * One transaction per request: user lock -> idempotency key -> KYC check -> fund check -> order row
 * -> ledger row -> idempotency response. Everything commits together or not at all.
 */
public class OrderService {

    public enum OrderStatus {
        PLACED, PAYMENT_PENDING, PAYMENT_SUCCESS,
        ORDER_SUBMITTED, ALLOTTED, FAILED, REFUNDED
    }

    public record Order(
        UUID orderId, UUID userId, UUID fundId,
        long amountPaise, OrderStatus status,
        String idempotencyKey, Instant createdAt
    ) {}

    /** Same key reused with a different payload or by a different user. Map to HTTP 409. */
    public static class IdempotencyConflictException extends RuntimeException {
        public IdempotencyConflictException(String m) { super(m); }
    }

    /** Swap in a HikariCP DataSource later with: new OrderService(dataSource::getConnection) */
    @FunctionalInterface
    public interface ConnectionProvider { Connection get() throws SQLException; }

    private final ConnectionProvider db;

    public OrderService(ConnectionProvider db) { this.db = db; }

    /** Reads DB_URL (jdbc:postgresql://host:5432/db), DB_USER, DB_PASSWORD from the environment. */
    public static ConnectionProvider fromEnv() {
        String url = require("DB_URL"), user = require("DB_USER"), pw = require("DB_PASSWORD");
        return () -> DriverManager.getConnection(url, user, pw);
    }

    private static String require(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing env var " + name);
        return v;
    }

    // ---------- pure logic (testable without a database) ----------

    public static void validateInput(String idempotencyKey, long amountPaise) {
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("Idempotency-Key header is mandatory.");
        if (idempotencyKey.length() > 255)
            throw new IllegalArgumentException("Idempotency-Key too long.");
        if (amountPaise <= 0)
            throw new IllegalArgumentException("Amount must be positive.");
    }

    public static String requestHash(UUID userId, UUID fundId, long amountPaise) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                .digest((userId + "|" + fundId + "|" + amountPaise).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString(); // 64 chars, fits VARCHAR(64)
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- database path ----------

    public Order createOrder(String idempotencyKey, UUID userId, UUID fundId, long amountPaise) {
        validateInput(idempotencyKey, amountPaise);
        String hash = requestHash(userId, fundId, amountPaise);
        try (Connection c = db.get()) {
            c.setAutoCommit(false);
            try {
                Order o = createInTx(c, idempotencyKey, userId, fundId, amountPaise, hash);
                c.commit();
                return o;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error while creating order", e);
        }
    }

    private Order createInTx(Connection c, String key, UUID userId, UUID fundId,
                             long amountPaise, String hash) throws SQLException {
        // 1. Lock the user row. Serializes concurrent requests for the same user,
        //    which keeps the running ledger balance correct.
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE id = ? FOR UPDATE")) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Unknown user.");
            }
        }

        // 2. Claim the idempotency key first.
        int inserted;
        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO idempotency_keys (key, user_id, request_hash, expires_at) " +
            "VALUES (?, ?, ?, NOW() + INTERVAL '24 hours') ON CONFLICT (key) DO NOTHING")) {
            ps.setString(1, key);
            ps.setObject(2, userId);
            ps.setString(3, hash);
            inserted = ps.executeUpdate();
        }
        if (inserted == 0) {
            // Replay (or conflict). The earlier request is already committed.
            try (PreparedStatement ps = c.prepareStatement(
                "SELECT user_id, request_hash FROM idempotency_keys WHERE key = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (!userId.equals(rs.getObject(1, UUID.class)) || !hash.equals(rs.getString(2)))
                        throw new IdempotencyConflictException(
                            "Idempotency-Key was already used with a different request.");
                }
            }
            return loadByKey(c, key);
        }

        // 3. KYC check on the server, from the database (not a caller-supplied flag).
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT status::text FROM kyc_profiles WHERE user_id = ?")) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !"VERIFIED".equalsIgnoreCase(rs.getString(1)))
                    throw new SecurityException("KYC must be VERIFIED before order placement.");
            }
        }

        // 4. Minimum & active status come from the fund.
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT min_investment_paise, is_active FROM funds WHERE id = ?")) {
            ps.setObject(1, fundId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Unknown fund.");
                if (!rs.getBoolean("is_active"))
                    throw new IllegalStateException("Fund is currently inactive for investments.");
                long min = rs.getLong("min_investment_paise");
                if (amountPaise < min)
                    throw new IllegalArgumentException("Minimum for this fund is " + min + " paise.");
            }
        }

        // 5. Order row.
        UUID orderId;
        Instant createdAt;
        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO orders (user_id, fund_id, amount_paise, status, idempotency_key) " +
            "VALUES (?, ?, ?, ?::order_status_enum, ?) RETURNING id, created_at")) {
            ps.setObject(1, userId);
            ps.setObject(2, fundId);
            ps.setLong(3, amountPaise);
            ps.setString(4, OrderStatus.PAYMENT_PENDING.name());
            ps.setString(5, key);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                orderId = rs.getObject(1, UUID.class);
                createdAt = rs.getTimestamp(2).toInstant();
            }
        }

        // 6. Ledger: running balance read inside the same transaction. DEBIT lowers it.
        long previous = 0;
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT balance_paise FROM transactions_ledger WHERE user_id = ? ORDER BY id DESC LIMIT 1")) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) previous = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO transactions_ledger " +
            "(user_id, order_id, transaction_type, direction, amount_paise, balance_paise, description) " +
            "VALUES (?, ?, 'INVESTMENT_DEBIT', 'DEBIT'::ledger_direction_enum, ?, ?, ?)")) {
            ps.setObject(1, userId);
            ps.setObject(2, orderId);
            ps.setLong(3, amountPaise);
            ps.setLong(4, previous - amountPaise);
            ps.setString(5, "Investment order " + orderId);
            ps.executeUpdate();
        }

        // 7. Store the response so later replays are provably identical.
        try (PreparedStatement ps = c.prepareStatement(
            "UPDATE idempotency_keys SET response_status = 201, response_body = ?::jsonb WHERE key = ?")) {
            ps.setString(1, "{\"order_id\":\"" + orderId + "\",\"status\":\"PAYMENT_PENDING\"}");
            ps.setString(2, key);
            ps.executeUpdate();
        }

        return new Order(orderId, userId, fundId, amountPaise,
            OrderStatus.PAYMENT_PENDING, key, createdAt);
    }

    private Order loadByKey(Connection c, String key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT id, user_id, fund_id, amount_paise, status::text, idempotency_key, created_at " +
            "FROM orders WHERE idempotency_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("Idempotency key exists but order is missing.");
                return new Order(
                    rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class),
                    rs.getLong(4), OrderStatus.valueOf(rs.getString(5)),
                    rs.getString(6), rs.getTimestamp(7).toInstant());
            }
        }
    }
}
