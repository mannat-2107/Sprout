package backend;

import backend.KycWebhookHandler.KycEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Signature checks run offline.
 * DB checks need docker compose up + DB_URL/DB_USER/DB_PASSWORD.
 */
public class KycWebhookCheck {
    static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static int failures = 0;

    static void check(boolean ok, String name) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Exception {
        String secret = "test-secret";
        String body = "{\"event_id\":\"x\"}";
        OrderService.ConnectionProvider noDb = () -> { throw new java.sql.SQLException("no db"); };
        KycWebhookHandler offline = new KycWebhookHandler(noDb, secret);
        KycEvent dummy = new KycEvent("x", USER, "PAN", "VERIFIED", null);

        String good = KycWebhookHandler.hmacHex(secret.getBytes(), body);
        check(offline.signatureValid(body, good), "valid signature accepted");
        check(!offline.signatureValid(body + " ", good), "tampered body rejected");
        check(!offline.signatureValid(body, KycWebhookHandler.hmacHex("other".getBytes(), body)), "wrong secret rejected");
        check(!offline.signatureValid(body, null), "missing signature rejected");
        try {
            offline.handle("cvl", body, "bad", dummy);
            check(false, "bad signature throws");
        } catch (SecurityException e) {
            check(true, "bad signature throws before touching the database");
        }

        if (System.getenv("DB_URL") == null) {
            System.out.println("\nDB_URL not set: skipped database checks.");
            System.out.println(failures == 0 ? "ALL OFFLINE CHECKS PASSED" : failures + " CHECK(S) FAILED");
            System.exit(failures == 0 ? 0 : 1);
        }

        KycWebhookHandler h = new KycWebhookHandler(OrderService.fromEnv(), secret);
        String id = "evt-" + UUID.randomUUID();
        String raw = "{\"event_id\":\"" + id + "\"}";
        String sig = KycWebhookHandler.hmacHex(secret.getBytes(), raw);
        KycEvent ev = new KycEvent(id, USER, "PAN", "VERIFIED", null);

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<KycWebhookHandler.Result>> fs = new ArrayList<>();
        for (int i = 0; i < 10; i++) fs.add(pool.submit(() -> { go.await(); return h.handle("cvl", raw, sig, ev); }));
        go.countDown();
        int processed = 0;
        for (var f : fs) if (f.get() == KycWebhookHandler.Result.PROCESSED) processed++;
        check(processed == 1, "same event delivered 10x concurrently is processed exactly once");

        String id2 = "evt-" + UUID.randomUUID();
        String raw2 = "{\"event_id\":\"" + id2 + "\"}";
        h.handle("cvl", raw2, KycWebhookHandler.hmacHex(secret.getBytes(), raw2),
                 new KycEvent(id2, USER, "PAN", "FAILED", "PAN_MISMATCH"));
        try (Connection c = OrderService.fromEnv().get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT s.status::text FROM kyc_steps s JOIN kyc_profiles p ON p.id = s.kyc_profile_id " +
                 "WHERE p.user_id = ? AND s.step_type = 'PAN'::kyc_step_enum")) {
            ps.setObject(1, USER);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                check("VERIFIED".equals(rs.getString(1)), "late FAILED event does not undo VERIFIED");
            }
        }
        pool.shutdown();
        System.out.println(failures == 0 ? "\nALL CHECKS PASSED" : "\n" + failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
