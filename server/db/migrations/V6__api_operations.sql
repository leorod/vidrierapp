-- API operational support: browser-token de-duplication, soft IP limits, and demo shops.

CREATE TABLE IF NOT EXISTS vote_rate_limits (
    ip_hash TEXT NOT NULL,
    window_started_at TIMESTAMPTZ NOT NULL,
    request_count INT NOT NULL DEFAULT 1,
    PRIMARY KEY (ip_hash, window_started_at)
);

CREATE INDEX IF NOT EXISTS idx_votes_browser_token_shop
    ON votes (browser_token, shop_id, created_at DESC)
    WHERE browser_token IS NOT NULL;

INSERT INTO shops (id, address_text, location, attributes, created_by) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Av. Juan Bautista Alberdi 6200, Mataderos', ST_SetSRID(ST_MakePoint(-58.5030, -34.6570), 4326)::geography, '{"neighborhood":"Mataderos","surface_m2":75,"rental_price":"ARS 950.000","agency":"Inmobiliaria Mataderos"}', 'seed'),
    ('22222222-2222-2222-2222-222222222222', 'Av. Rivadavia 16100, Haedo', ST_SetSRID(ST_MakePoint(-58.5930, -34.6420), 4326)::geography, '{"neighborhood":"Haedo","surface_m2":58,"rental_price":"ARS 720.000","agency":"Oeste Propiedades"}', 'seed')
ON CONFLICT (id) DO NOTHING;

COMMENT ON TABLE vote_rate_limits IS 'Ventanas de rate limiting suave por IP hasheada; complementa huella y token de navegador sin bloquear CGNAT por una sola solicitud.';
