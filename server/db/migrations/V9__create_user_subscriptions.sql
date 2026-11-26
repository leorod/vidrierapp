CREATE TABLE IF NOT EXISTS subscriptions (
    user_id UUID PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    status trial_status NOT NULL DEFAULT 'ACTIVE_TRIAL',
    trial_started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    trial_expires_at TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '3 days',
    pass_activated_at TIMESTAMPTZ,
    pass_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

ALTER TABLE mock_payments ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES app_users(id) ON DELETE SET NULL;
ALTER TABLE mock_payments ALTER COLUMN device_id DROP NOT NULL;
CREATE INDEX IF NOT EXISTS idx_mock_payments_user_created_at ON mock_payments (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_subscriptions_expires ON subscriptions (trial_expires_at, pass_expires_at);

COMMENT ON TABLE subscriptions IS 'Trial y pase de 30 días asociados a una cuenta de usuario autenticada.';
