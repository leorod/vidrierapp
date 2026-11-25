-- ─────────────────────────────────────────────────────────────
-- V4 — Índices adicionales, vistas y validaciones de rendimiento
-- Ref: BE-01 (< 50 ms bounding-box), RF01, RF02, RF03
-- ─────────────────────────────────────────────────────────────

-- ── 1. Índice espacial compuesto: location + status ────────────
-- Permite filtrar locales AVAILABLE dentro de un bounding-box en un
-- único scan GIST sin re-filtrar por status en memoria.
CREATE INDEX IF NOT EXISTS idx_shops_location_status
    ON shops USING GIST (location)
    WHERE status = 'AVAILABLE';

-- ── 2. Vista materializada: conteo de votos por local ──────────
-- Evita COUNT(*) on-the-fly en cada request de GET /shops.
-- Se refresca periódicamente o tras cada batch de votos.

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_shop_vote_summary AS
SELECT
    v.shop_id,
    COUNT(*)                                            AS vote_count,
    COUNT(*) FILTER (WHERE v.category_status = 'CLASSIFIED') AS classified_count,
    MIN(v.created_at)                                   AS first_vote_at,
    MAX(v.created_at)                                   AS last_vote_at,
    -- Rubro con más votos (top category)
    MODE() WITHIN GROUP (ORDER BY v.category)           AS top_category,
    -- ¿La muestra supera el umbral de 15 votos? (DET-01, RF03)
    COUNT(*) >= 15                                      AS is_sample_sufficient
FROM votes v
GROUP BY v.shop_id
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_shop_vote_summary_pk
    ON mv_shop_vote_summary (shop_id);

-- ── 3. Vista de distribución horaria por local ─────────────────
-- Alimenta la sección de franjas horarias de la ficha de detalle (DET-01).

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_shop_hourly_distribution AS
SELECT
    shop_id,
    day_of_week,
    hour_bucket,
    COUNT(*) AS vote_count
FROM votes
WHERE category_status = 'CLASSIFIED'
GROUP BY shop_id, day_of_week, hour_bucket
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_shop_hourly_pk
    ON mv_shop_hourly_distribution (shop_id, day_of_week, hour_bucket);

CREATE INDEX IF NOT EXISTS idx_mv_shop_hourly_shop
    ON mv_shop_hourly_distribution (shop_id);

-- ── 4. Función helper: shops dentro de un bounding-box ─────────
-- Cubre el caso de uso de RF01 (mapa) y el criterio de < 50 ms (BE-01).
-- Uso:
--   SELECT * FROM fn_shops_in_bbox(-58.55, -34.70, -58.45, -34.60, 'AVAILABLE');

CREATE OR REPLACE FUNCTION fn_shops_in_bbox(
    lon_min  DOUBLE PRECISION,
    lat_min  DOUBLE PRECISION,
    lon_max  DOUBLE PRECISION,
    lat_max  DOUBLE PRECISION,
    p_status shop_status DEFAULT 'AVAILABLE'
)
RETURNS TABLE (
    id                   UUID,
    address_text         TEXT,
    status               shop_status,
    location             GEOGRAPHY,
    attributes           JSONB,
    last_verified_at     TIMESTAMPTZ,
    vote_count           BIGINT,
    top_category         TEXT,
    is_sample_sufficient BOOLEAN
)
LANGUAGE sql STABLE PARALLEL SAFE AS $$
    SELECT
        s.id,
        s.address_text,
        s.status,
        s.location,
        s.attributes,
        s.last_verified_at,
        COALESCE(vs.vote_count, 0),
        vs.top_category,
        COALESCE(vs.is_sample_sufficient, FALSE)
    FROM shops s
    LEFT JOIN mv_shop_vote_summary vs ON vs.shop_id = s.id
    WHERE
        s.status = p_status
        AND s.location && ST_MakeEnvelope(lon_min, lat_min, lon_max, lat_max, 4326)::geography
    ORDER BY s.id;
$$;

COMMENT ON FUNCTION fn_shops_in_bbox IS
    'Retorna locales dentro del bounding-box dado con métricas de votos precalculadas. '
    'Usa ST_MakeEnvelope + índice GIST para cumplir < 50 ms (BE-01).';

-- ── 5. Función helper: votos dentro del límite de rate-limit ───
-- Verifica si ya existe un voto del mismo fingerprint para un local
-- en los últimos 30 días (BE-02).

CREATE OR REPLACE FUNCTION fn_vote_is_duplicate(
    p_fingerprint TEXT,
    p_shop_id     UUID
)
RETURNS BOOLEAN
LANGUAGE sql STABLE AS $$
    SELECT EXISTS (
        SELECT 1
        FROM votes
        WHERE fingerprint_hash = p_fingerprint
          AND shop_id           = p_shop_id
          AND created_at       >= NOW() - INTERVAL '30 days'
    );
$$;

COMMENT ON FUNCTION fn_vote_is_duplicate IS
    'Devuelve TRUE si el fingerprint ya emitió un voto para ese local en los últimos 30 días (rate-limit anti-duplicados, BE-02).';
