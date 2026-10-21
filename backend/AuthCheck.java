package backend;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Offline checks always run. DB checks need DB_URL/DB_USER/DB_PASSWORD and 002_auth.sql applied. */
public class AuthCheck {
    static int failures = 0;
    static void check(boolean ok, String name) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Exception {
        String secret = "a-test-secret-that-is-at-least-32-chars!!";
        OrderService.ConnectionProvider noDb = () -> { throw new java.sql.SQLException("no db"); };
        AuthService offline = new AuthService(noDb, secret, (p, o) -> {});
        UUID u = UUID.randomUUID();
        long now = 1_000_000L;

        String t = offline.issueAccessToken(u, now);
        check(offline.verifyAccessToken(t, now + 10).equals(Optional.of(u)), "valid access token accepted");
        check(offline.verifyAccessToken(t, now + AuthService.ACCESS_TTL_SEC + 1).isEmpty(), "expired access token rejected");
        String[] p = t.split("\\.");
        String forged = UUID.randomUUID() + "." + p[1] + "." + p[2];
        check(offline.verifyAccessToken(forged, now + 10).isEmpty(), "token for a different user id rejected");
        check(offline.verifyAccessToken("garbage", now).isEmpty(), "garbage token rejected");

        for (String bad : new String[]{"9876543210", "+919876543210 ", "+915876543210", "+91987654321"}) {
            try { AuthService.validatePhone(bad); check(false, "rejects phone " + bad); }
            catch (IllegalArgumentException e) { check(true, "rejects phone '" + bad + "'"); }
        }
        AuthService.validatePhone("+919876543210");
        check(AuthService.containsSensitiveIds("{\"pan\":\"ABCDE1234F\"}"), "PAN in answers detected");
        check(AuthService.containsSensitiveIds("{\"x\":\"1234 5678 9012\"}"), "Aadhaar-like number detected");
        check(!AuthService.containsSensitiveIds("{\"goal\":\"tea\",\"pace\":\"daily\",\"amount\":20}"), "normal answers allowed");
        try { new AuthService(noDb, "short", (a, b) -> {}); check(false, "short secret refused"); }
        catch (IllegalStateException e) { check(true, "short secret refused"); }

        if (System.getenv("DB_URL") == null) {
            System.out.println("\nDB_URL not set: skipped database checks.");
            System.out.println(failures == 0 ? "ALL OFFLINE CHECKS PASSED" : failures + " CHECK(S) FAILED");
            System.exit(failures == 0 ? 0 : 1);
        }

        AtomicReference<String> lastOtp = new AtomicReference<>();
        AuthService svc = new AuthService(OrderService.fromEnv(), secret, (ph, otp) -> lastOtp.set(otp));
        String phone = "+91" + (6 + new Random().nextInt(4)) + String.format("%09d", new Random().nextInt(1_000_000_000));

        svc.requestOtp(phone);
        String good = lastOtp.get();
        String wrong = good.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 5; i++) svc.verifyOtp(phone, wrong, "test");
        check(svc.verifyOtp(phone, good, "test").isEmpty(), "correct code refused after 5 wrong attempts");

        svc.requestOtp(phone);
        good = lastOtp.get();
        Optional<AuthService.Tokens> t1 = svc.verifyOtp(phone, good, "test");
        check(t1.isPresent(), "correct code logs in");
        check(svc.verifyOtp(phone, good, "test").isEmpty(), "same code cannot be used twice");
        svc.requestOtp(phone);
        try { svc.requestOtp(phone); check(false, "4th code request in 15 minutes limited"); }
        catch (AuthService.RateLimitException e) { check(true, "4th code request in 15 minutes limited"); }

        AuthService.Tokens a = t1.get();
        check(svc.verifyAccessToken(a.accessToken()).equals(Optional.of(a.userId())), "access token verifies for the user");
        Optional<AuthService.Tokens> r1 = svc.refresh(a.refreshToken(), "test");
        check(r1.isPresent() && !r1.get().refreshToken().equals(a.refreshToken()), "refresh issues a new token");
        check(svc.refresh(a.refreshToken(), "test").isEmpty(), "old refresh token rejected (reuse)");
        check(svc.refresh(r1.get().refreshToken(), "test").isEmpty(), "reuse also revoked the newer token in the chain");

        svc.saveProgress(a.userId(), 3, "{\"pace\":\"daily\",\"amount\":20}");
        AuthService.Progress pr = svc.getProgress(a.userId());
        check(pr.currentScreen().equals("3") && pr.answersJson().contains("daily"), "progress saved and resumed");

        System.out.println(failures == 0 ? "\nALL CHECKS PASSED" : "\n" + failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
