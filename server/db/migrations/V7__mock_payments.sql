-- Simulated payment provider audit trail for the 30-day access pass.

CREATE TABLE IF NOT EXISTS mock_payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id TEXT NOT NULL,
    provider_reference TEXT NOT NULL UNIQUE,
    amount_cents INT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'ARS',
    status TEXT NOT NULL CHECK (status IN ('APPROVED', 'DECLINED')),
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mock_payments_device_created_at
    ON mock_payments (device_id, created_at DESC);

COMMENT ON TABLE mock_payments IS 'Intentos de checkout simulados para el pase de 30 días. No almacena datos de tarjeta ni se conecta a un procesador real.';
