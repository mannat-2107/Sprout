-- Auth additions.
-- Schema baseline: users(id, phone UNIQUE), sessions(user_id, hashed_refresh_token, device_info JSONB, expires_at, revoked_at),
-- onboarding_progress(user_id PRIMARY KEY, current_screen INT, answers JSONB, updated_at).

-- OTP codes are stored only as keyed hashes, never in plain text.
CREATE TABLE IF NOT EXISTS otp_challenges (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone       VARCHAR(20) NOT NULL,
    otp_hash    VARCHAR(64) NOT NULL,
    attempts    INT NOT NULL DEFAULT 0,
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_otp_phone_created ON otp_challenges(phone, created_at DESC);

-- Groups a refresh token with every token rotated from it, so reuse can revoke the whole chain.
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS family_id UUID;
CREATE INDEX IF NOT EXISTS idx_sessions_family ON sessions(family_id);
-- In schema.sql the column name is hashed_refresh_token
CREATE UNIQUE INDEX IF NOT EXISTS idx_sessions_token_hash ON sessions(hashed_refresh_token);
