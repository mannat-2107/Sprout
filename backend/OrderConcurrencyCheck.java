package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.*;

/**
 * Run against a REAL Postgres (docker compose up -d). Env: DB_URL, DB_USER, DB_PASSWORD.
 * Uses the dev seed user and fund. Exits non-zero on any failed check.
 */
public class OrderConcurrencyCheck {
    static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID FUND = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static int failures = 0;

    static void check(boolean ok, String name) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Exception {
        OrderService svc = new OrderService(OrderService.fromEnv());
        ExecutorService pool = Executors.newFixedThreadPool(20);

        // 1. Same key from 20 threads at once -> exactly one order, one ledger row.
        String key = "test-" + UUID.randomUUID();
        CountDownLatch go = new CountDownLatch(1);
        List<Future<OrderService.Order>> fs = new ArrayList<>();
        for (int i = 0; i < 20; i++)
            fs.add(pool.submit(() -> { go.await(); return svc.createOrder(key, USER, FUND, 10_000); }));
        go.countDown();
        Set<UUID> ids = new HashSet<>();
        for (var f : fs) ids.add(f.get().orderId());
        check(ids.size() == 1, "20 concurrent same-key requests return one order id");
        check(count("SELECT count(*) FROM orders WHERE idempotency_key = '" + key + "'") == 1, "one order row");
        check(count("SELECT count(*) FROM transactions_ledger WHERE order_id = '" + ids.iterator().next() + "'") == 1,
              "one ledger row");

        // 2. Same key, different amount -> conflict.
        try { svc.createOrder(key, USER, FUND, 20_000); check(false, "payload mismatch rejected"); }
        catch (OrderService.IdempotencyConflictException e) { check(true, "payload mismatch rejected"); }

        // 3. Below fund minimum -> rejected, and key is not consumed.
        String lowKey = "low-" + UUID.randomUUID();
        try { svc.createOrder(lowKey, USER, FUND, 5_000); check(false, "below-minimum rejected"); }
        catch (IllegalArgumentException e) { check(true, "below-minimum rejected"); }
        check(count("SELECT count(*) FROM idempotency_keys WHERE key = '" + lowKey + "'") == 0,
              "failed request leaves no idempotency row");

        // 4. 20 different keys at once for the same user -> balance chain stays consistent.
        long before = balance();
        List<Future<OrderService.Order>> many = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String k = "multi-" + UUID.randomUUID();
            many.add(pool.submit(() -> svc.createOrder(k, USER, FUND, 10_000)));
        }
        for (var f : many) f.get();
        check(balance() == before - 20 * 10_000L, "balance dropped by exactly 20 x 10000 paise");
        check(chainIsConsistent(), "every ledger row = previous balance minus its amount");

        pool.shutdown();
        System.out.println(failures == 0 ? "\nALL CHECKS PASSED" : "\n" + failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    static long count(String sql) throws Exception {
        try (Connection c = OrderService.fromEnv().get(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
    }

    static long balance() throws Exception {
        try (Connection c = OrderService.fromEnv().get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT balance_paise FROM transactions_ledger WHERE user_id = ? ORDER BY id DESC LIMIT 1")) {
            ps.setObject(1, USER);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 0; }
        }
    }

    static boolean chainIsConsistent() throws Exception {
        try (Connection c = OrderService.fromEnv().get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT direction::text, amount_paise, balance_paise FROM transactions_ledger " +
                 "WHERE user_id = ? ORDER BY id")) {
            ps.setObject(1, USER);
            try (ResultSet rs = ps.executeQuery()) {
                long prev = 0;
                while (rs.next()) {
                    long expected = "DEBIT".equals(rs.getString(1)) ? prev - rs.getLong(2) : prev + rs.getLong(2);
                    if (expected != rs.getLong(3)) return false;
                    prev = rs.getLong(3);
                }
                return true;
            }
        }
    }
}
