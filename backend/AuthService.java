package backend;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Phone + OTP login, short-lived access tokens, rotating refresh tokens with reuse detection,
 * and server-saved onboarding progress.
 *
 * Access tokens here are a small HMAC-signed format to avoid dependencies. When you add a build
 * tool, replace them with a vetted JWT library.
 */
public class AuthService {

    public record Tokens(UUID userId, String accessToken, long accessExpiresAtEpoch, String refreshToken) {}
    public record Progress(String currentScreen, String answersJson) {}

    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String m) { super(m); }
    }

    public interface OtpSender { void send(String phone, String otp); }

    /** Dev only. Refuses to run unless ALLOW_DEV_OTP_LOG=true, so OTPs can't leak into prod logs. */
    public static OtpSender devConsoleSender() {
        return (phone, otp) -> {
            if (!"true".equals(System.getenv("ALLOW_DEV_OTP_LOG")))
                throw new IllegalStateException("No SMS provider configured.");
            System.out.println("[DEV ONLY] OTP for " + phone + ": " + otp);
        };
    }

    static final int OTP_MAX_ATTEMPTS = 5, OTP_MAX_REQUESTS = 3, OTP_TTL_MIN = 5;
    static final int ACCESS_TTL_SEC = 15 * 60, REFRESH_TTL_DAYS = 30;
    static final Pattern PHONE = Pattern.compile("^\\+91[6-9]\\d{9}$");
    // PAN, or a 12-digit Aadhaar-like number (spaces allowed). Must never sit in onboarding answers.
    static final Pattern SENSITIVE = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]|\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b");
    static final SecureRandom RNG = new SecureRandom();

    private final OrderService.ConnectionProvider db;
    private final byte[] secret;
    private final OtpSender sender;

    public AuthService(OrderService.ConnectionProvider db, String authSecret, OtpSender sender) {
        if (authSecret == null || authSecret.length() < 32)
            throw new IllegalStateException("AUTH_SECRET must be at least 32 characters.");
        this.db = db;
        this.secret = authSecret.getBytes(StandardCharsets.UTF_8);
        this.sender = sender;
    }

    // ---------- pure helpers ----------

    public static void validatePhone(String phone) {
        if (phone == null || !PHONE.matcher(phone).matches())
            throw new IllegalArgumentException("Enter a valid Indian mobile number in +91XXXXXXXXXX format.");
    }

    public static boolean containsSensitiveIds(String text) {
        return text != null && SENSITIVE.matcher(text).find();
    }

    private String mac(String label, String data) {
        return KycWebhookHandler.hmacHex(secret, label + ":" + data);
    }

    String issueAccessToken(UUID userId, long nowEpoch) {
        String payload = userId + "." + (nowEpoch + ACCESS_TTL_SEC);
        return payload + "." + mac("access", payload);
    }

    public Optional<UUID> verifyAccessToken(String token) {
        return verifyAccessToken(token, System.currentTimeMillis() / 1000);
    }

    Optional<UUID> verifyAccessToken(String token, long nowEpoch) {
        try {
            if (token == null) return Optional.empty();
            String[] p = token.split("\\.");
            if (p.length != 3) return Optional.empty();
            String payload = p[0] + "." + p[1];
            boolean ok = MessageDigest.isEqual(
                mac("access", payload).getBytes(StandardCharsets.UTF_8), p[2].getBytes(StandardCharsets.UTF_8));
            if (!ok || Long.parseLong(p[1]) < nowEpoch) return Optional.empty();
            return Optional.of(UUID.fromString(p[0]));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static String sha256Hex(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static String randomToken() {
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    // ---------- transaction helper ----------

    @FunctionalInterface private interface TxWork<T> { T run(Connection c) throws SQLException; }

    private <T> T inTx(TxWork<T> w) {
        try (Connection c = db.get()) {
            c.setAutoCommit(false);
            try {
                T r = w.run(c);
                c.commit();
                return r;
            } catch (RuntimeException | SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error", e);
        }
    }

    private static void lockPhone(Connection c, String phone) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
            ps.setString(1, "otp:" + phone);
            ps.execute();
        }
    }

    // ---------- OTP ----------

    public void requestOtp(String phone) {
        validatePhone(phone);
        String otp = String.format("%06d", RNG.nextInt(1_000_000));
        UUID id = UUID.randomUUID();
        inTx(c -> {
            lockPhone(c, phone);
            try (PreparedStatement ps = c.prepareStatement(
                "SELECT count(*) FROM otp_challenges WHERE phone = ? AND created_at > NOW() - INTERVAL '15 minutes'")) {
                ps.setString(1, phone);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getLong(1) >= OTP_MAX_REQUESTS)
                        throw new RateLimitException("Too many codes requested. Please try again in a few minutes.");
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO otp_challenges (id, phone, otp_hash, expires_at) " +
                "VALUES (?, ?, ?, NOW() + (? * INTERVAL '1 minute'))")) {
                ps.setObject(1, id);
                ps.setString(2, phone);
                ps.setString(3, mac("otp", id + ":" + phone + ":" + otp));
                ps.setInt(4, OTP_TTL_MIN);
                ps.executeUpdate();
            }
            return null;
        });
        sender.send(phone, otp); // after commit
    }

    /** Empty means "invalid or expired code". One answer for every failure so nothing leaks. */
    public Optional<Tokens> verifyOtp(String phone, String otp, String deviceInfo) {
        validatePhone(phone);
        if (otp == null || !otp.matches("\\d{6}")) return Optional.empty();
        return inTx(c -> {
            lockPhone(c, phone);
            UUID challengeId;
            String storedHash;
            int attempts;
            try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, otp_hash, attempts FROM otp_challenges " +
                "WHERE phone = ? AND consumed_at IS NULL AND expires_at > NOW() " +
                "ORDER BY created_at DESC LIMIT 1 FOR UPDATE")) {
                ps.setString(1, phone);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.<Tokens>empty();
                    challengeId = rs.getObject(1, UUID.class);
                    storedHash = rs.getString(2);
                    attempts = rs.getInt(3);
                }
            }
            if (attempts >= OTP_MAX_ATTEMPTS) return Optional.<Tokens>empty();

            boolean match = MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                mac("otp", challengeId + ":" + phone + ":" + otp).getBytes(StandardCharsets.UTF_8));
            if (!match) {
                try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE otp_challenges SET attempts = attempts + 1 WHERE id = ?")) {
                    ps.setObject(1, challengeId);
                    ps.executeUpdate();
                }
                return Optional.<Tokens>empty(); // commits, so the attempt count sticks
            }
            try (PreparedStatement ps = c.prepareStatement(
                "UPDATE otp_challenges SET consumed_at = NOW() WHERE id = ?")) {
                ps.setObject(1, challengeId);
                ps.executeUpdate();
            }

            UUID userId;
            try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users (phone) VALUES (?) ON CONFLICT (phone) DO UPDATE SET phone = EXCLUDED.phone RETURNING id")) {
                ps.setString(1, phone);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); userId = rs.getObject(1, UUID.class); }
            }
            try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO onboarding_progress (user_id) VALUES (?) ON CONFLICT (user_id) DO NOTHING")) {
                ps.setObject(1, userId);
                ps.executeUpdate();
            }
            return Optional.of(issueSession(c, userId, deviceInfo, UUID.randomUUID()));
        });
    }

    // ---------- sessions ----------

    private Tokens issueSession(Connection c, UUID userId, String deviceInfo, UUID familyId) throws SQLException {
        String refresh = randomToken();
        // Column is hashed_refresh_token, device_info is JSONB in schema.sql
        String jsonDevice = (deviceInfo == null || deviceInfo.isBlank())
            ? null
            : "{\"client\":\"" + deviceInfo.replace("\"", "\\\"") + "\"}";

        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO sessions (user_id, hashed_refresh_token, family_id, device_info, expires_at) " +
            "VALUES (?, ?, ?, ?::jsonb, NOW() + (? * INTERVAL '1 day'))")) {
            ps.setObject(1, userId);
            ps.setString(2, sha256Hex(refresh));
            ps.setObject(3, familyId);
            ps.setString(4, jsonDevice);
            ps.setInt(5, REFRESH_TTL_DAYS);
            ps.executeUpdate();
        }
        long now = System.currentTimeMillis() / 1000;
        return new Tokens(userId, issueAccessToken(userId, now), now + ACCESS_TTL_SEC, refresh);
    }

    /** Rotates the refresh token. Presenting an already-used token revokes the whole chain. */
    public Optional<Tokens> refresh(String refreshToken, String deviceInfo) {
        if (refreshToken == null || refreshToken.isBlank()) return Optional.empty();
        String hash = sha256Hex(refreshToken);
        return inTx(c -> {
            UUID userId, familyId;
            boolean expired, revoked;
            try (PreparedStatement ps = c.prepareStatement(
                "SELECT user_id, family_id, expires_at < NOW(), revoked_at IS NOT NULL " +
                "FROM sessions WHERE hashed_refresh_token = ? FOR UPDATE")) {
                ps.setString(1, hash);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.<Tokens>empty();
                    userId = rs.getObject(1, UUID.class);
                    familyId = rs.getObject(2, UUID.class);
                    expired = rs.getBoolean(3);
                    revoked = rs.getBoolean(4);
                }
            }
            if (revoked) { // reuse of a rotated token: assume it leaked, end every session in the chain
                revokeFamily(c, familyId);
                return Optional.<Tokens>empty();
            }
            if (expired) return Optional.<Tokens>empty();
            try (PreparedStatement ps = c.prepareStatement(
                "UPDATE sessions SET revoked_at = NOW() WHERE hashed_refresh_token = ?")) {
                ps.setString(1, hash);
                ps.executeUpdate();
            }
            return Optional.of(issueSession(c, userId, deviceInfo, familyId));
        });
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String hash = sha256Hex(refreshToken);
        inTx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                "SELECT family_id FROM sessions WHERE hashed_refresh_token = ?")) {
                ps.setString(1, hash);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) revokeFamily(c, rs.getObject(1, UUID.class));
                }
            }
            return null;
        });
    }

    private static void revokeFamily(Connection c, UUID familyId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "UPDATE sessions SET revoked_at = NOW() WHERE family_id = ? AND revoked_at IS NULL")) {
            ps.setObject(1, familyId);
            ps.executeUpdate();
        }
    }

    // ---------- onboarding progress (always scoped to the user id from a verified token) ----------

    public Progress getProgress(UUID userId) {
        return inTx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                "SELECT current_screen::text, answers::text FROM onboarding_progress WHERE user_id = ?")) {
                ps.setObject(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return new Progress("1", "{}");
                    String a = rs.getString(2);
                    return new Progress(rs.getString(1), a == null ? "{}" : a);
                }
            }
        });
    }

    public void saveProgress(UUID userId, int screen, String answersJson) {
        if (screen < 1 || screen > 5) throw new IllegalArgumentException("Screen must be 1 to 5.");
        if (answersJson == null || answersJson.length() > 10_000)
            throw new IllegalArgumentException("Answers missing or too large.");
        if (containsSensitiveIds(answersJson))
            throw new IllegalArgumentException("Do not store PAN or Aadhaar numbers in onboarding answers.");
        inTx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                "UPDATE onboarding_progress SET current_screen = ?, answers = ?::jsonb, updated_at = NOW() WHERE user_id = ?")) {
                ps.setInt(1, screen);
                ps.setString(2, answersJson);
                ps.setObject(3, userId);
                if (ps.executeUpdate() == 0) throw new IllegalStateException("No onboarding record for user.");
            }
            return null;
        });
    }
}
