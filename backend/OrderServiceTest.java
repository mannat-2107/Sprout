package backend;

import java.util.Objects;
import java.util.UUID;

/**
 * Sprout 2.0 Pure Unit Tests (Offline / No DB required)
 * Tests:
 * - OrderService.validateInput constraints
 * - OrderService.requestHash determinism & SHA-256 formatting
 * - KycStateMachine state transition matrix & terminal states
 * - KycStateMachine HMAC-SHA256 cryptographic verification
 */
public class OrderServiceTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" Running Sprout 2.0 Unit Tests (Offline Logic)   ");
        System.out.println("==================================================");

        testValidateInputSuccess();
        testValidateInputFailures();
        testRequestHashDeterminism();
        testKycStateMachineTransitions();
        testKycHmacVerification();

        System.out.println("==================================================");
        System.out.println(" ALL 5 UNIT TEST SUITES PASSED (0 FAILURES)      ");
        System.out.println("==================================================");
    }

    private static void testValidateInputSuccess() {
        System.out.print("[TEST 1] Testing validateInput with valid parameters... ");
        OrderService.validateInput("valid-idempotency-key-123", 10_000L);
        System.out.println("PASSED");
    }

    private static void testValidateInputFailures() {
        System.out.print("[TEST 2] Testing validateInput boundary conditions... ");
        
        // Null or blank key
        assertThrows(() -> OrderService.validateInput(null, 10_000L), IllegalArgumentException.class);
        assertThrows(() -> OrderService.validateInput("   ", 10_000L), IllegalArgumentException.class);

        // Long key (> 255 chars)
        String longKey = "k".repeat(256);
        assertThrows(() -> OrderService.validateInput(longKey, 10_000L), IllegalArgumentException.class);

        // Non-positive amount
        assertThrows(() -> OrderService.validateInput("key-1", 0L), IllegalArgumentException.class);
        assertThrows(() -> OrderService.validateInput("key-1", -500L), IllegalArgumentException.class);

        System.out.println("PASSED");
    }

    private static void testRequestHashDeterminism() {
        System.out.print("[TEST 3] Testing SHA-256 requestHash formatting & determinism... ");
        UUID u = UUID.randomUUID();
        UUID f = UUID.randomUUID();
        String h1 = OrderService.requestHash(u, f, 15_000L);
        String h2 = OrderService.requestHash(u, f, 15_000L);
        String h3 = OrderService.requestHash(u, f, 20_000L);

        assertEquals(h1, h2, "Identical request arguments must yield identical hash");
        assertEquals(64, h1.length(), "SHA-256 hex string must be exactly 64 characters");
        assertTrue(!h1.equals(h3), "Different amount must yield different hash");
        System.out.println("PASSED");
    }

    private static void testKycStateMachineTransitions() {
        System.out.print("[TEST 4] Testing KYC State Machine transitions... ");

        // Valid transition
        KycStateMachine.KycState s1 = KycStateMachine.validateTransition(
            KycStateMachine.KycState.NOT_STARTED, KycStateMachine.KycState.IN_PROGRESS);
        assertEquals(KycStateMachine.KycState.IN_PROGRESS, s1, "Transition to IN_PROGRESS allowed");

        // Invalid direct jump: NOT_STARTED to VERIFIED
        assertThrows(() -> KycStateMachine.validateTransition(
            KycStateMachine.KycState.NOT_STARTED, KycStateMachine.KycState.VERIFIED), IllegalStateException.class);

        // Terminal state: VERIFIED cannot transition to anything
        assertThrows(() -> KycStateMachine.validateTransition(
            KycStateMachine.KycState.VERIFIED, KycStateMachine.KycState.IN_PROGRESS), IllegalStateException.class);

        System.out.println("PASSED");
    }

    private static void testKycHmacVerification() {
        System.out.print("[TEST 5] Testing KYC HMAC-SHA256 signature verification... ");
        String secret = "test_webhook_secret_key";
        KycStateMachine sm = new KycStateMachine(secret);
        String payload = "{\"event_id\":\"evt_101\",\"status\":\"VERIFIED\"}";

        // Compute valid signature
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            String validSig = sb.toString();

            assertTrue(sm.verifySignature(payload, validSig), "Valid signature must pass");
            assertTrue(!sm.verifySignature(payload, "invalid_sig_hex"), "Invalid signature must fail");
            assertTrue(!sm.verifySignature(payload + "tampered", validSig), "Tampered payload must fail");
            assertTrue(!sm.verifySignature(payload, null), "Null signature must fail");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("PASSED");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    interface RunnableWithException {
        void run() throws Exception;
    }

    private static void assertThrows(RunnableWithException action, Class<? extends Throwable> expectedType) {
        try {
            action.run();
            throw new AssertionError("Expected " + expectedType.getSimpleName() + " was not thrown.");
        } catch (Throwable t) {
            if (!expectedType.isInstance(t)) {
                throw new AssertionError("Expected " + expectedType.getSimpleName() + " but got " + t.getClass().getSimpleName(), t);
            }
        }
    }
}
