CREATE TABLE device_trials (
    device_id_hash  text PRIMARY KEY,
    started_at      timestamptz NOT NULL DEFAULT now(),
    expires_at      timestamptz NOT NULL,
    CONSTRAINT device_trials_length_chk CHECK (expires_at = started_at + interval '3 days')
);

CREATE INDEX device_trials_expires_idx ON device_trials (expires_at);

CREATE TABLE pass_purchases (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id_hash   text NOT NULL,
    purchased_at     timestamptz NOT NULL DEFAULT now(),
    expires_at       timestamptz NOT NULL,
    simulated        boolean NOT NULL DEFAULT true,
    CONSTRAINT pass_purchases_length_chk CHECK (expires_at = purchased_at + interval '30 days')
);

CREATE INDEX pass_purchases_device_expires_idx ON pass_purchases (device_id_hash, expires_at DESC);

COMMENT ON TABLE device_trials IS 'PRIMARY KEY on the device hash allows exactly one 3-day trial per device';
COMMENT ON TABLE pass_purchases IS 'Simulated 30-day pass, no payment gateway';
