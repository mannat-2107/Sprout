package backend;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Sprout 2.0 KYC Webhook Handler & State Machine (Postgres-backed)
 * Handles:
 * - Cryptographic HMAC-SHA256 signature verification
 * - Exactly-once event deduplication via webhook_events
 * - Strictly guarded state transitions
 * - Atomic persistence across kyc_profiles, kyc_steps, kyc_events, and webhook_events
 * - Plain-language failure mapping for Indian fintech regulatory compliance
 */
public class KycStateMachine {

    public enum KycState {
        NOT_STARTED,
        IN_PROGRESS,
        PENDING,
        VERIFIED,
        REJECTED,
        NEEDS_RETRY
    }

    public enum KycStep {
        PAN,
        AADHAAR_DIGILOCKER,
        SELFIE,
        BANK_PENNY_DROP
    }

    // Allowed transition map (Prevents invalid jumps e.g. REJECTED -> VERIFIED without retry)
    private static final Map<KycState, Set<KycState>> ALLOWED_TRANSITIONS = new EnumMap<>(KycState.class);
    static {
        ALLOWED_TRANSITIONS.put(KycState.NOT_STARTED, EnumSet.of(KycState.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(KycState.IN_PROGRESS, EnumSet.of(KycState.PENDING, KycState.VERIFIED, KycState.NEEDS_RETRY, KycState.REJECTED));
        ALLOWED_TRANSITIONS.put(KycState.PENDING, EnumSet.of(KycState.VERIFIED, KycState.NEEDS_RETRY, KycState.REJECTED));
        ALLOWED_TRANSITIONS.put(KycState.NEEDS_RETRY, EnumSet.of(KycState.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(KycState.REJECTED, Collections.emptySet()); // Terminal state requiring human escalation
        ALLOWED_TRANSITIONS.put(KycState.VERIFIED, Collections.emptySet()); // Terminal state (Success)
    }

    // Plain-language resolution mapping for vendor failure codes
    private static final Map<String, String> FAILURE_CODE_MAP = new HashMap<>();
    static {
        FAILURE_CODE_MAP.put("PAN_NAME_MISMATCH", "The name on your PAN card differs slightly from your bank records. Please check your spelling.");
        FAILURE_CODE_MAP.put("UIDAI_TIMEOUT", "Aadhaar e-KYC server timed out. We will automatically retry in the background.");
        FAILURE_CODE_MAP.put("BANK_PENNY_REJECTED", "Your bank account could not be validated for ₹1 penny drop. Please confirm your IFSC code.");
        FAILURE_CODE_MAP.put("IMAGE_BLURRY", "The uploaded document was unreadable. Please capture the document in clear lighting.");
    }

    @FunctionalInterface
    public interface ConnectionProvider { Connection get() throws SQLException; }

    private final ConnectionProvider db;
    private final String webhookSecret;

    public KycStateMachine(String webhookSecret) {
        this(fromEnv(), webhookSecret);
    }

    public KycStateMachine(ConnectionProvider db, String webhookSecret) {
        this.db = db;
        this.webhookSecret = webhookSecret != null ? webhookSecret : "";
    }

    public static ConnectionProvider fromEnv() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pw = System.getenv("DB_PASSWORD");
        if (url == null || url.isBlank()) {
            return () -> null; // in-memory fallback if not provided
        }
        return () -> DriverManager.getConnection(url, user, pw);
    }

    /**
     * Verifies cryptographic HMAC-SHA256 signature sent by KYC provider.
     */
    public boolean verifySignature(String payload, String expectedSignature) {
        if (expectedSignature == null || expectedSignature.isBlank() || webhookSecret.isBlank()) {
            return false;
        }
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equalsIgnoreCase(expectedSignature.trim());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }

    /**
     * Validates transition according to allowed transition matrix.
     */
    public static KycState validateTransition(KycState currentState, KycState targetState) {
        Set<KycState> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentState, Collections.emptySet());
        if (!allowed.contains(targetState)) {
            throw new IllegalStateException(String.format("Invalid KYC transition attempted: %s -> %s", currentState, targetState));
        }
        return targetState;
    }

    /**
     * Atomically processes an inbound provider webhook:
     * 1. Validates HMAC signature
     * 2. Claims event_id in webhook_events (deduplication)
     * 3. Locks kyc_profiles row FOR UPDATE
     * 4. Validates state transition
     * 5. Updates kyc_profiles and kyc_steps
     * 6. Inserts audit trail in kyc_events
     * 7. Marks webhook_events as PROCESSED
     */
    public WebhookResult processWebhook(
        String provider,
        String eventId,
        String eventType,
        String rawPayload,
        String signature,
        UUID userId,
        KycStep stepType,
        String failureCode,
        KycState targetState
    ) {
        if (!verifySignature(rawPayload, signature)) {
            return new WebhookResult(false, "INVALID_SIGNATURE", "Cryptographic signature validation failed.", null);
        }

        try (Connection c = db.get()) {
            if (c == null) {
                throw new IllegalStateException("Database connection not configured.");
            }
            c.setAutoCommit(false);
            try {
                // 1. Claim event in webhook_events
                int inserted;
                try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO webhook_events (provider, event_id, event_type, payload, status) " +
                    "VALUES (?, ?, ?, ?::jsonb, 'RECEIVED') ON CONFLICT (event_id) DO NOTHING")) {
                    ps.setString(1, provider != null ? provider : "KYC_PROVIDER");
                    ps.setString(2, eventId);
                    ps.setString(3, eventType != null ? eventType : "KYC_UPDATE");
                    ps.setString(4, (rawPayload != null && !rawPayload.isBlank()) ? rawPayload : "{}");
                    inserted = ps.executeUpdate();
                }

                if (inserted == 0) {
                    // Duplicate event replay check
                    c.commit();
                    return new WebhookResult(true, "DUPLICATE_IGNORED", "Event already processed previously.", null);
                }

                // 2. Lock and load user's KYC profile
                UUID kycProfileId;
                KycState currentState;
                try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, status::text FROM kyc_profiles WHERE user_id = ? FOR UPDATE")) {
                    ps.setObject(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("No KYC profile found for user " + userId);
                        }
                        kycProfileId = rs.getObject(1, UUID.class);
                        currentState = KycState.valueOf(rs.getString(2));
                    }
                }

                // 3. Transition check
                KycState newState = validateTransition(currentState, targetState);
                String userMessage = FAILURE_CODE_MAP.getOrDefault(failureCode, "Verification processed.");

                // 4. Update kyc_profiles
                try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE kyc_profiles SET status = ?::kyc_status_enum, " +
                    "rejection_reason = ?, " +
                    "verified_at = CASE WHEN ?::kyc_status_enum = 'VERIFIED' THEN NOW() ELSE verified_at END, " +
                    "updated_at = NOW() WHERE id = ?")) {
                    ps.setString(1, newState.name());
                    ps.setString(2, failureCode != null ? userMessage : null);
                    ps.setString(3, newState.name());
                    ps.setObject(4, kycProfileId);
                    ps.executeUpdate();
                }

                // 5. Update kyc_steps if step specified
                if (stepType != null) {
                    String stepStatus = (newState == KycState.VERIFIED) ? "VERIFIED" :
                                        (newState == KycState.REJECTED || newState == KycState.NEEDS_RETRY) ? "FAILED" : "PENDING";
                    try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO kyc_steps (kyc_profile_id, step_type, status, attempts, failure_reason_code, failure_message, updated_at) " +
                        "VALUES (?, ?::kyc_step_enum, ?, 1, ?, ?, NOW()) " +
                        "ON CONFLICT (kyc_profile_id, step_type) DO UPDATE SET " +
                        "status = EXCLUDED.status, " +
                        "attempts = kyc_steps.attempts + 1, " +
                        "failure_reason_code = EXCLUDED.failure_reason_code, " +
                        "failure_message = EXCLUDED.failure_message, " +
                        "updated_at = NOW()")) {
                        ps.setObject(1, kycProfileId);
                        ps.setString(2, stepType.name());
                        ps.setString(3, stepStatus);
                        ps.setString(4, failureCode);
                        ps.setString(5, failureCode != null ? userMessage : null);
                        ps.executeUpdate();
                    }
                }

                // 6. Record audit log in kyc_events
                try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO kyc_events (kyc_profile_id, step_type, from_status, to_status, note) " +
                    "VALUES (?, ?::kyc_step_enum, ?, ?, ?)")) {
                    ps.setObject(1, kycProfileId);
                    if (stepType != null) {
                        ps.setString(2, stepType.name());
                    } else {
                        ps.setNull(2, java.sql.Types.VARCHAR);
                    }
                    ps.setString(3, currentState.name());
                    ps.setString(4, newState.name());
                    ps.setString(5, "Webhook event: " + eventId + (failureCode != null ? (" (" + failureCode + ")") : ""));
                    ps.executeUpdate();
                }

                // 7. Mark webhook_events as PROCESSED
                try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE webhook_events SET status = 'PROCESSED', processed_at = NOW() WHERE event_id = ?")) {
                    ps.setString(1, eventId);
                    ps.executeUpdate();
                }

                c.commit();
                return new WebhookResult(true, "PROCESSED", userMessage, newState);

            } catch (Exception e) {
                c.rollback();

                // Record failure status on webhook event in separate transaction
                try (Connection errConn = db.get()) {
                    try (PreparedStatement ps = errConn.prepareStatement(
                        "UPDATE webhook_events SET status = 'FAILED', error_message = ? WHERE event_id = ?")) {
                        ps.setString(1, e.getMessage());
                        ps.setString(2, eventId);
                        ps.executeUpdate();
                    }
                } catch (Exception ignored) {}

                return new WebhookResult(false, "PROCESSING_ERROR", e.getMessage(), null);
            }
        } catch (SQLException e) {
            return new WebhookResult(false, "DATABASE_ERROR", e.getMessage(), null);
        }
    }

    public record WebhookResult(boolean success, String code, String message, KycState resultingState) {}
}
