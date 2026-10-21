-- DEV/TEST ONLY. Never run against staging or production.
-- Populates baseline entities for OrderConcurrencyCheck and local dev.

INSERT INTO users (id, phone, status, preferred_language)
VALUES ('11111111-1111-1111-1111-111111111111', '+910000000001', 'ACTIVE', 'en')
ON CONFLICT (id) DO NOTHING;

INSERT INTO funds (id, provider_code, name, description, category, riskometer_level, min_investment_paise, is_active)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'TEST_FUND',
    'Test Fund (dev only)',
    'Dev test index fund for concurrency and order lifecycle verification',
    'INDEX_FUND',
    'VERY_HIGH',
    10000,
    TRUE
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO kyc_profiles (user_id, status, pan_verified, aadhaar_verified, verified_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'VERIFIED',
    TRUE,
    TRUE,
    NOW()
)
ON CONFLICT (user_id) DO NOTHING;
