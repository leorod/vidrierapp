-- ─────────────────────────────────────────────────────────────
-- V3 — Tabla `device_trials`
-- Ref: BE-01, DET-03, ADR §1.2, RNF-07
-- Garantiza 1 único trial de 3 días por dispositivo físico.
-- ─────────────────────────────────────────────────────────────

-- Estado del trial / suscripción
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'trial_status') THEN
        CREATE TYPE trial_status AS ENUM ('ACTIVE_TRIAL', 'TRIAL_EXPIRED', 'PASS_ACTIVE', 'PASS_EXPIRED');
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS device_trials (
    -- ── Identidad ──────────────────────────────────────────────
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- ── Identificación del dispositivo ────────────────────────
    -- Hardware ID / UUID generado en el primer arranque de la app Android.
    -- No contiene PII; es el identificador único de instalación.
    device_id           TEXT            NOT NULL UNIQUE,

    -- ── Estado y ciclo de vida ─────────────────────────────────
    status              trial_status    NOT NULL DEFAULT 'ACTIVE_TRIAL',

    -- Momento en que se activó el trial de 3 días
    trial_started_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- Fecha de expiración calculada al insertar (trial_started_at + 3 días).
    -- No se usa GENERATED ALWAYS porque NOW() no es inmutable en PG.
    trial_expires_at    TIMESTAMPTZ     NOT NULL,

    -- ── Pase de 30 días (opcional) ─────────────────────────────
    -- Nulos hasta que el usuario compra el pase (DET-03)
    pass_activated_at   TIMESTAMPTZ,
    -- Calculado cuando se activa el pase
    pass_expires_at     TIMESTAMPTZ,

    -- ── Auditoría ──────────────────────────────────────────────
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ
);

-- ── Trigger: calcular trial_expires_at en INSERT ───────────────

CREATE OR REPLACE FUNCTION set_trial_expires_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.trial_expires_at = NEW.trial_started_at + INTERVAL '3 days';
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_device_trials_expires
    BEFORE INSERT ON device_trials
    FOR EACH ROW EXECUTE FUNCTION set_trial_expires_at();

-- ── Trigger: updated_at ────────────────────────────────────────

CREATE TRIGGER trg_device_trials_updated_at
    BEFORE UPDATE ON device_trials
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── Índices ────────────────────────────────────────────────────

-- Lookup rápido por device_id (endpoint POST /auth/trial)
CREATE INDEX IF NOT EXISTS idx_device_trials_device_id
    ON device_trials (device_id);

-- Consultas de expiración (job nocturno que actualiza estados)
CREATE INDEX IF NOT EXISTS idx_device_trials_expires
    ON device_trials (trial_expires_at, pass_expires_at);

-- ── Restricción: una sola fila por device_id ───────────────────
-- La columna ya tiene UNIQUE; aquí se documenta la semántica:
-- Si device_id ya existe → devolver el registro existente (no crear uno nuevo).
-- La app Android verifica localmente; el backend valida contra esta tabla.

-- ── Comentarios ────────────────────────────────────────────────

COMMENT ON TABLE  device_trials                 IS 'Registro del trial de 3 días y pase de 30 días por dispositivo único (ADR §1.2).';
COMMENT ON COLUMN device_trials.device_id       IS 'UUID generado en el primer arranque de la app Android. No contiene PII. Es la clave de unicidad del trial.';
COMMENT ON COLUMN device_trials.trial_expires_at IS 'trial_started_at + 3 días. Se calcula con un trigger BEFORE INSERT para evitar la restricción de inmutabilidad de PG en columnas generadas.';
COMMENT ON COLUMN device_trials.pass_expires_at  IS 'NULL hasta que el usuario compra el Pase de 30 días. No tiene renovación automática (ADR §1.2).';
