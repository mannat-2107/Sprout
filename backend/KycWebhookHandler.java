package backend;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Postgres-backed KYC webhook handling matching Sprout 2.0 schema.sql.
 * Flow: verify HMAC on the RAW body -> store raw event in webhook_events (dedupe) -> lock profile
 * -> update step in kyc_steps -> recompute profile status in kyc_profiles -> mark processed.
 */
public class KycWebhookHandler {

    public record KycEvent(String eventId, UUID userId, String step, String status, String failureCode) {}

    public enum Result { PROCESSED, DUPLICATE }

    /**
     * Steps that must be VERIFIED in Sprout schema:
     * Enum: 'PAN', 'AADHAAR_DIGILOCKER', 'SELFIE', 'BANK_PENNY_DROP'
     * For minimum investment eligibility: PAN, AADHAAR_DIGILOCKER, BANK_PENNY_DROP (or PAN for quick test).
     */
    static final List<String> REQUIRED_STEPS = List.of("PAN", "AADHAAR_DIGILOCKER", "BANK_PENNY_DROP");

    private final OrderService.ConnectionProvider db;
    private final byte[] secret;

    public KycWebhookHandler(OrderService.ConnectionProvider db, String webhookSecret) {
        if (webhookSecret == null || webhookSecret.isBlank())
            throw new IllegalStateException("KYC webhook secret is required.");
        this.db = db;
        this.secret = webhookSecret.getBytes(StandardCharsets.UTF_8);
    }

    // ---------- signature ----------

    public static String hmacHex(byte[] key, String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] d = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean signatureValid(String rawBody, String signatureHex) {
        if (signatureHex == null) return false;
        byte[] expected = hmacHex(secret, rawBody).getBytes(StandardCharsets.UTF_8);
        byte[] given = signatureHex.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, given); // constant-time
    }

    // ---------- friendly messages for Indian fintech vendor failure codes ----------

    static final Map<String, String> FRIENDLY = Map.of(
        "PAN_MISMATCH",      "The PAN doesn't match the name on your account. Please re-check it and try again.",
        "IMAGE_BLURRY",      "The photo was unclear. Please retake it in good light.",
        "PROVIDER_TIMEOUT",  "The verification service is slow right now. We'll keep trying and notify you.",
        "UIDAI_TIMEOUT",     "Aadhaar e-KYC server timed out. We will automatically retry in the background.",
        "BANK_PENNY_REJECTED", "Your bank account could not be validated for ₹1 penny drop. Please confirm your IFSC code."
    );

    public static String friendlyMessage(String code) {
        return FRIENDLY.getOrDefault(code, "We couldn't verify this step. Please try again or contact support.");
    }

    // ---------- handling ----------

    public Result handle(String provider, String rawBody, String signatureHex, KycEvent e) {
        if (!signatureValid(rawBody, signatureHex))
            throw new SecurityException("Invalid webhook signature."); // HTTP 401, store nothing
        try (Connection c = db.get()) {
            c.setAutoCommit(false);
            try {
                Result r = process(c, provider, rawBody, e);
                c.commit();
                return r;
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Database error handling KYC webhook", ex);
        }
    }

    private Result process(Connection c, String provider, String rawBody, KycEvent e) throws SQLException {
        // 1. Store raw event in webhook_events. event_type is NOT NULL in schema.sql.
        int inserted;
        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO webhook_events (provider, event_id, event_type, payload, status) " +
            "VALUES (?, ?, 'KYC_STEP_UPDATE', ?::jsonb, 'RECEIVED') ON CONFLICT (event_id) DO NOTHING")) {
            ps.setString(1, provider);
            ps.setString(2, e.eventId());
            ps.setString(3, rawBody);
            inserted = ps.executeUpdate();
        }
        if (inserted == 0) return Result.DUPLICATE;

        // 2. Lock this user's profile so concurrent events apply one at a time.
        // kyc_profiles.id is UUID in schema.sql
        UUID profileId;
        String currentProfileStatus;
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT id, status::text FROM kyc_profiles WHERE user_id = ? FOR UPDATE")) {
            ps.setObject(1, e.userId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("No KYC profile for user " + e.userId());
                profileId = rs.getObject(1, UUID.class);
                currentProfileStatus = rs.getString(2);
            }
        }

        // Normalize step to schema enum if shortcut used
        String stepName = e.step();
        if ("AADHAAR".equalsIgnoreCase(stepName)) stepName = "AADHAAR_DIGILOCKER";
        if ("BANK".equalsIgnoreCase(stepName)) stepName = "BANK_PENNY_DROP";

        // 3. Apply to the step. VERIFIED is sticky: a late or stale event never undoes it.
        String existing = null;
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT status::text FROM kyc_steps WHERE kyc_profile_id = ? AND step_type = ?::kyc_step_enum")) {
            ps.setObject(1, profileId);
            ps.setString(2, stepName);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) existing = rs.getString(1); }
        }

        boolean failed = "FAILED".equals(e.status()) || "NEEDS_RETRY".equals(e.status()) || "REJECTED".equals(e.status());
        String failureMsg = failed && e.failureCode() != null ? friendlyMessage(e.failureCode()) : null;

        if (existing == null) {
            try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO kyc_steps (kyc_profile_id, step_type, status, attempts, failure_reason_code, failure_message) " +
                "VALUES (?, ?::kyc_step_enum, ?, ?, ?, ?)")) {
                ps.setObject(1, profileId);
                ps.setString(2, stepName);
                ps.setString(3, e.status());
                ps.setInt(4, failed ? 1 : 0);
                ps.setString(5, failed ? e.failureCode() : null);
                ps.setString(6, failureMsg);
                ps.executeUpdate();
            }
        } else if (!"VERIFIED".equals(existing)) {
            try (PreparedStatement ps = c.prepareStatement(
                "UPDATE kyc_steps SET status = ?, attempts = attempts + ?, failure_reason_code = ?, failure_message = ?, updated_at = NOW() " +
                "WHERE kyc_profile_id = ? AND step_type = ?::kyc_step_enum")) {
                ps.setString(1, e.status());
                ps.setInt(2, failed ? 1 : 0);
                ps.setString(3, failed ? e.failureCode() : null);
                ps.setString(4, failureMsg);
                ps.setObject(5, profileId);
                ps.setString(6, stepName);
                ps.executeUpdate();
            }
        }

        // 4. Recompute profile status from all steps.
        List<String> statuses = new ArrayList<>();
        List<String> verifiedSteps = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT step_type::text, status::text FROM kyc_steps WHERE kyc_profile_id = ?")) {
            ps.setObject(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    statuses.add(rs.getString(2));
                    if ("VERIFIED".equals(rs.getString(2))) verifiedSteps.add(rs.getString(1));
                }
            }
        }

        String next;
        if ("VERIFIED".equalsIgnoreCase(currentProfileStatus)) next = "VERIFIED"; // sticky
        else if (verifiedSteps.containsAll(REQUIRED_STEPS)) next = "VERIFIED";
        else if (statuses.contains("FAILED") || statuses.contains("REJECTED")) next = "NEEDS_RETRY";
        else if (!statuses.isEmpty()) next = "IN_PROGRESS";
        else next = "PENDING";

        if (!next.equalsIgnoreCase(currentProfileStatus)) {
            try (PreparedStatement ps = c.prepareStatement(
                "UPDATE kyc_profiles SET status = ?::kyc_status_enum, " +
                "verified_at = CASE WHEN ? = 'VERIFIED' THEN NOW() ELSE verified_at END, " +
                "updated_at = NOW() WHERE id = ?")) {
                ps.setString(1, next);
                ps.setString(2, next);
                ps.setObject(3, profileId);
                ps.executeUpdate();
            }
        }

        // 5. Audit event in kyc_events
        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO kyc_events (kyc_profile_id, step_type, from_status, to_status, note) " +
            "VALUES (?, ?::kyc_step_enum, ?, ?, ?)")) {
            ps.setObject(1, profileId);
            ps.setString(2, stepName);
            ps.setString(3, currentProfileStatus);
            ps.setString(4, next);
            ps.setString(5, "Webhook " + e.eventId() + " - step status " + e.status());
            ps.executeUpdate();
        }

        // 6. Mark raw event processed in webhook_events.
        try (PreparedStatement ps = c.prepareStatement(
            "UPDATE webhook_events SET status = 'PROCESSED', processed_at = NOW() WHERE provider = ? AND event_id = ?")) {
            ps.setString(1, provider);
            ps.setString(2, e.eventId());
            ps.executeUpdate();
        }
        return Result.PROCESSED;
    }
}
