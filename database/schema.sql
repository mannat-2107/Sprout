-- ============================================================================
-- SPROUT 2.0 PRODUCTION POSTGRESQL SCHEMA (PHASES 1 TO 3)
-- Compliant with SEBI, AMFI, and DPDP Act Data Minimization Standards.
-- Integer Paise Architecture: All currency stored as BIGINT in paise (1 INR = 100 paise).
-- Zero floating-point arithmetic on monetary balances.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. AUTHENTICATION, USERS & SAVED PROGRESS (PHASE 1)
-- ============================================================================

CREATE TYPE user_status_enum AS ENUM ('ACTIVE', 'BLOCKED', 'DELETED');
CREATE TYPE kyc_status_enum AS ENUM ('NOT_STARTED', 'IN_PROGRESS', 'PENDING', 'VERIFIED', 'REJECTED', 'NEEDS_RETRY');
CREATE TYPE kyc_step_enum AS ENUM ('PAN', 'AADHAAR_DIGILOCKER', 'SELFIE', 'BANK_PENNY_DROP');
CREATE TYPE order_status_enum AS ENUM ('PLACED', 'PAYMENT_PENDING', 'PAYMENT_SUCCESS', 'ORDER_SUBMITTED', 'ALLOTTED', 'FAILED', 'REFUNDED');
CREATE TYPE payment_status_enum AS ENUM ('INITIATED', 'SUCCESS', 'FAILED', 'REFUNDED');
CREATE TYPE ledger_direction_enum AS ENUM ('DEBIT', 'CREDIT');

-- Core Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(15) NOT NULL UNIQUE,
    status user_status_enum NOT NULL DEFAULT 'ACTIVE',
    preferred_language VARCHAR(5) NOT NULL DEFAULT 'en',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_phone ON users(phone);

-- Login Sessions & Refresh Token Rotation
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hashed_refresh_token VARCHAR(255) NOT NULL,
    device_info JSONB,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_sessions_user ON sessions(user_id) WHERE revoked_at IS NULL;

-- OTP Rate Limiting & Lockout Defense
CREATE TABLE otp_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(15) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 1,
    window_start TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    lockout_until TIMESTAMPTZ,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_otp_phone_window ON otp_attempts(phone, window_start);

-- Cross-Device "Pause and Come Back" Onboarding Progress
CREATE TABLE onboarding_progress (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    current_screen INT NOT NULL DEFAULT 1 CHECK (current_screen BETWEEN 1 AND 5),
    answers JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Versioned Compliance & Terms Consents
CREATE TABLE consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL, -- 'RISK_DISCLAIMER', 'TERMS_OF_SERVICE', 'PRIVACY_POLICY'
    document_version VARCHAR(20) NOT NULL,
    ip_address INET,
    user_agent TEXT,
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_consents_user_doc ON consents(user_id, document_type, document_version);

-- Append-Only Audit Log for Sensitive Actions
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    before_state JSONB,
    after_state JSONB,
    ip_address INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);

-- Idempotency Registry (Guarantees Exactly-Once Order and Payment Creation)
CREATE TABLE idempotency_keys (
    key VARCHAR(255) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    request_hash VARCHAR(64) NOT NULL,
    response_status INT,
    response_body JSONB,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);

-- Webhook Ingestion & Deduplication
CREATE TABLE webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(50) NOT NULL, -- 'CVL_KRA', 'CAMS', 'RAZORPAY_UPI', 'BSE_STAR_MF'
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED', -- 'RECEIVED', 'PROCESSED', 'FAILED'
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_webhook_status ON webhook_events(status, provider);

-- ============================================================================
-- 2. KYC PROFILES & VERIFICATION PIPELINE (PHASE 2)
-- ============================================================================

CREATE TABLE kyc_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    status kyc_status_enum NOT NULL DEFAULT 'NOT_STARTED',
    pan_encrypted BYTEA,             -- Encrypted at rest using pgp_sym_encrypt or application AES-256
    pan_masked VARCHAR(10),          -- ABCDE****F
    pan_verified BOOLEAN NOT NULL DEFAULT FALSE,
    aadhaar_ref_id VARCHAR(100),     -- UIDAI transaction reference token (Never raw 12-digit Aadhaar!)
    aadhaar_masked VARCHAR(4),       -- Last 4 digits only
    aadhaar_verified BOOLEAN NOT NULL DEFAULT FALSE,
    provider_ref VARCHAR(100),
    rejection_reason TEXT,
    verified_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_kyc_status ON kyc_profiles(status);

CREATE TABLE kyc_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kyc_profile_id UUID NOT NULL REFERENCES kyc_profiles(id) ON DELETE CASCADE,
    step_type kyc_step_enum NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    failure_reason_code VARCHAR(50),
    failure_message TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(kyc_profile_id, step_type)
);

CREATE TABLE kyc_events (
    id BIGSERIAL PRIMARY KEY,
    kyc_profile_id UUID NOT NULL REFERENCES kyc_profiles(id) ON DELETE CASCADE,
    step_type kyc_step_enum,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_kyc_events_profile ON kyc_events(kyc_profile_id);

CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_number_encrypted BYTEA NOT NULL,
    account_number_masked VARCHAR(8) NOT NULL, -- e.g. ****4821
    ifsc_code VARCHAR(11) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_holder_name VARCHAR(150) NOT NULL,
    penny_drop_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    penny_drop_ref VARCHAR(100),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_bank_accounts_user ON bank_accounts(user_id);

-- ============================================================================
-- 3. FUNDS, ORDERS & DOUBLE-ENTRY LEDGER (PHASE 3)
-- ============================================================================

CREATE TABLE funds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_code VARCHAR(50) NOT NULL UNIQUE, -- BSE Star MF scheme code (e.g. 'INF209K01157')
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'INDEX_FUND',
    riskometer_level VARCHAR(30) NOT NULL DEFAULT 'VERY_HIGH',
    min_investment_paise BIGINT NOT NULL DEFAULT 10000 CHECK (min_investment_paise >= 1000), -- ₹100 min
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    fund_id UUID NOT NULL REFERENCES funds(id),
    amount_paise BIGINT NOT NULL CHECK (amount_paise >= 1000), -- Minimum ₹10 (1000 paise)
    status order_status_enum NOT NULL DEFAULT 'PLACED',
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    provider_order_ref VARCHAR(100),
    allotment_units NUMERIC(18, 4),
    allotted_at TIMESTAMPTZ,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_orders_user_status ON orders(user_id, status);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    status payment_status_enum NOT NULL DEFAULT 'INITIATED',
    provider_txn_id VARCHAR(100) UNIQUE,
    amount_paise BIGINT NOT NULL CHECK (amount_paise > 0),
    upi_vpa VARCHAR(100),
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payments_order ON payments(order_id);

CREATE TABLE order_events (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status order_status_enum,
    to_status order_status_enum NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_order_events_order ON order_events(order_id);

CREATE TABLE holdings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fund_id UUID NOT NULL REFERENCES funds(id),
    units NUMERIC(18, 4) NOT NULL DEFAULT 0.0000 CHECK (units >= 0),
    invested_amount_paise BIGINT NOT NULL DEFAULT 0 CHECK (invested_amount_paise >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, fund_id)
);
CREATE INDEX idx_holdings_user ON holdings(user_id);

-- Strictly Append-Only Immutable Financial Ledger
CREATE TABLE transactions_ledger (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    order_id UUID REFERENCES orders(id),
    payment_id UUID REFERENCES payments(id),
    transaction_type VARCHAR(50) NOT NULL, -- 'INVESTMENT_DEBIT', 'UNITS_ALLOTTED', 'REFUND'
    direction ledger_direction_enum NOT NULL,
    amount_paise BIGINT NOT NULL CHECK (amount_paise > 0),
    balance_paise BIGINT NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ledger_user ON transactions_ledger(user_id, created_at DESC);

-- Trigger to Prevent Any UPDATE or DELETE on transactions_ledger (Enforces Immutability)
CREATE OR REPLACE FUNCTION enforce_ledger_immutability()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Financial Ledger is strictly append-only. Modifying or deleting records is prohibited by law.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_immutable
BEFORE UPDATE OR DELETE ON transactions_ledger
FOR EACH ROW EXECUTE FUNCTION enforce_ledger_immutability();

-- Daily Net Asset Value (NAV) Historical Tracking
CREATE TABLE nav_history (
    id BIGSERIAL PRIMARY KEY,
    fund_id UUID NOT NULL REFERENCES funds(id),
    nav_date DATE NOT NULL,
    nav_paise BIGINT NOT NULL CHECK (nav_paise > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(fund_id, nav_date)
);
CREATE INDEX idx_nav_fund_date ON nav_history(fund_id, nav_date DESC);
