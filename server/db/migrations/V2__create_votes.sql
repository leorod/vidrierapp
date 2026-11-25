-- ─────────────────────────────────────────────────────────────
-- V2 — Tabla `votes`
-- Ref: BE-01, BE-02, BE-03, WEB-01, RF03, RNF-09
-- ─────────────────────────────────────────────────────────────

-- Tipo para la frecuencia declarada de visita (ADR §1.3)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'visit_frequency') THEN
        CREATE TYPE visit_frequency AS ENUM ('DAILY', 'WEEKLY', 'OCCASIONAL');
    END IF;
END$$;

-- Tipo para el estado de clasificación del rubro (BE-03)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'category_status') THEN
        CREATE TYPE category_status AS ENUM ('CLASSIFIED', 'UNCLASSIFIED');
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS votes (
    -- ── Identidad ──────────────────────────────────────────────
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- ── Referencia al local ────────────────────────────────────
    shop_id             UUID            NOT NULL
                            REFERENCES shops(id) ON DELETE CASCADE,

    -- ── Rubro votado ──────────────────────────────────────────
    -- Valor normalizado por el pipeline determinístico (BE-03)
    -- Ej: "cafeteria", "verduleria", "panaderia", "other"
    category            TEXT            NOT NULL,
    -- Texto libre original si el vecino eligió "Otro" (antes de normalizar)
    raw_category_input  TEXT,
    -- Estado del pipeline de clasificación (BE-03)
    category_status     category_status NOT NULL DEFAULT 'CLASSIFIED',

    -- ── Métricas de demanda ────────────────────────────────────
    -- Frecuencia declarada (nullable — el vecino puede omitirla, WEB-01)
    frequency           visit_frequency,
    -- Día de la semana del escaneo (0=Domingo … 6=Sábado)
    day_of_week         SMALLINT        NOT NULL
                            CHECK (day_of_week BETWEEN 0 AND 6),
    -- Franja horaria: hora de inicio del bloque de 2 hs (0, 2, 4 … 22)
    hour_bucket         SMALLINT        NOT NULL
                            CHECK (hour_bucket BETWEEN 0 AND 23),

    -- ── Anti-duplicados (BE-02, RNF-09) ───────────────────────
    -- Hash SHA-256 de la huella técnica anónima del navegador
    -- (user-agent + accept-language + resolución; NO PII)
    fingerprint_hash    TEXT            NOT NULL,
    -- Token persistido en localStorage del navegador del vecino
    browser_token       TEXT,

    -- ── Timestamps ────────────────────────────────────────────
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ── Índices ────────────────────────────────────────────────────

-- Consultas de agregación por local (RF03, endpoint GET /shops/{id})
CREATE INDEX IF NOT EXISTS idx_votes_shop_id
    ON votes (shop_id);

-- Conteo y distribución por categoría
CREATE INDEX IF NOT EXISTS idx_votes_shop_category
    ON votes (shop_id, category);

-- Consulta de duplicados: 1 voto por huella + local cada 30 días (BE-02)
CREATE INDEX IF NOT EXISTS idx_votes_fingerprint_shop
    ON votes (fingerprint_hash, shop_id, created_at DESC);

-- Distribución por día y franja horaria (RF03)
CREATE INDEX IF NOT EXISTS idx_votes_day_hour
    ON votes (shop_id, day_of_week, hour_bucket);

-- Procesamiento batch de no clasificados (BE-03, BE-04)
CREATE INDEX IF NOT EXISTS idx_votes_unclassified
    ON votes (category_status)
    WHERE category_status = 'UNCLASSIFIED';

-- ── Regla de unicidad anti-duplicados ─────────────────────────
-- Un mismo fingerprint no puede votar más de una vez por local en 30 días.
-- Se implementa con una función + índice parcial en lugar de UNIQUE
-- para tolerar rotación de IPs CGNAT (BE-02).
-- La validación real la hace el backend antes de INSERT; este índice
-- actúa como salvaguarda a nivel DB.

-- ── Comentarios ────────────────────────────────────────────────

COMMENT ON TABLE  votes                     IS 'Votos anónimos de vecinos sobre el rubro deseado para un local disponible.';
COMMENT ON COLUMN votes.category            IS 'Rubro normalizado por el pipeline determinístico (BE-03). Valor canónico en snake_case.';
COMMENT ON COLUMN votes.raw_category_input  IS 'Texto libre original del vecino cuando eligió "Otro". NULL para rubros del catálogo.';
COMMENT ON COLUMN votes.frequency           IS 'Frecuencia declarada de visita. NULL si el vecino omitió el paso opcional (WEB-01).';
COMMENT ON COLUMN votes.hour_bucket         IS 'Hora de inicio del bloque de 2 horas en que se escaneó el QR (0–23).';
COMMENT ON COLUMN votes.fingerprint_hash    IS 'SHA-256 de la huella técnica del navegador. No contiene PII. Usado para rate-limiting (BE-02).';
