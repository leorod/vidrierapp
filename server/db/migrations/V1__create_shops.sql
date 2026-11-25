-- ─────────────────────────────────────────────────────────────
-- V1 — Tabla `shops`
-- Ref: BE-01, RF03, RNF-07, RNF-09, ADR §4.1, ADR §4.3
-- ─────────────────────────────────────────────────────────────

-- Tipo enumerado para el estado del local
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'shop_status') THEN
        CREATE TYPE shop_status AS ENUM ('AVAILABLE', 'UNDER_REVIEW', 'RENTED');
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS shops (
    -- ── Identidad ──────────────────────────────────────────────
    id                      UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- ── Dirección y geolocalización ────────────────────────────
    -- Texto libre ingresado por el administrador
    address_text            TEXT            NOT NULL,
    -- place_id de Google; opcional, puede repetirse (ADR §4.3)
    place_id                TEXT,
    -- Punto elegido manualmente por el administrador sobre el mapa
    -- geography para cálculos de distancia en metros con ST_DWithin
    location                GEOGRAPHY(Point, 4326) NOT NULL,
    -- true si el admin aceptó una ubicación a > 150 m de la sugerencia
    location_override_used  BOOLEAN         NOT NULL DEFAULT FALSE,

    -- ── Ciclo de vida / estado ─────────────────────────────────
    status                  shop_status     NOT NULL DEFAULT 'AVAILABLE',
    -- Última vez que un admin revalidó el cartel (caduca a 60 días — WEB-02)
    last_verified_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- Recuento de reportes de "ya se alquiló" recibidos (REP-01)
    rented_report_count     INT             NOT NULL DEFAULT 0,

    -- ── Atributos dinámicos ────────────────────────────────────
    -- Superficie m², precio, inmobiliaria, teléfono, etc. (ADR §4.1)
    attributes              JSONB           NOT NULL DEFAULT '{}',

    -- ── Auditoría ──────────────────────────────────────────────
    created_by              TEXT            NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_by              TEXT,
    updated_at              TIMESTAMPTZ
);

-- ── Índices ────────────────────────────────────────────────────

-- Índice espacial GIST — soporta ST_DWithin y ST_MakeEnvelope (BE-01)
CREATE INDEX IF NOT EXISTS idx_shops_location
    ON shops USING GIST (location);

-- Índice sobre status para filtrar disponibles (RF01, RF02)
CREATE INDEX IF NOT EXISTS idx_shops_status
    ON shops (status);

-- Índice para la regla de caducidad (WEB-02)
CREATE INDEX IF NOT EXISTS idx_shops_last_verified_at
    ON shops (last_verified_at);

-- Índice GIN sobre JSONB para búsquedas por atributos dinámicos
CREATE INDEX IF NOT EXISTS idx_shops_attributes
    ON shops USING GIN (attributes);

-- ── Trigger: actualizar updated_at automáticamente ─────────────

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_shops_updated_at
    BEFORE UPDATE ON shops
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── Comentarios de columna ─────────────────────────────────────

COMMENT ON TABLE  shops                        IS 'Locales comerciales disponibles, en revisión o alquilados.';
COMMENT ON COLUMN shops.location               IS 'Coordenada WGS-84 (lon, lat) elegida manualmente por el administrador. Nunca proviene de la API de Geocoding de Google (ADR §4.3).';
COMMENT ON COLUMN shops.location_override_used IS 'true si el administrador confirmó una ubicación a > 150 m de la sugerencia de Google o usó la salida de emergencia.';
COMMENT ON COLUMN shops.last_verified_at       IS 'El administrador revalida el cartel cada 60 días; pasado ese plazo el estado pasa a UNDER_REVIEW (WEB-02).';
COMMENT ON COLUMN shops.attributes             IS 'Payload JSONB con campos variables: superficie_m2, precio_ars, inmobiliaria, telefono, email, fotos_urls, etc.';
