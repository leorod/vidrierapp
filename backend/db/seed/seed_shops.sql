TRUNCATE votes, shops, device_trials, pass_purchases RESTART IDENTITY CASCADE;

SELECT setseed(0.42) AS seeded \gset

INSERT INTO shops (id, name, address_text, neighborhood, top_category, latitude, longitude, status,
                   last_verified_at, surface_area_m2, rental_price_text, contact_agency, contact_phone,
                   contact_email, attributes)
VALUES
('shop-mataderos-01', 'Local Alberdi 6420', 'Av. Juan Bautista Alberdi 6420', 'Mataderos', 'cafeteria', -34.6525, -58.502, 'AVAILABLE',
 now() - interval '5 days', 75, '$850,000 / mes', 'Inmobiliaria Mataderos Prop.', '+54 11 4687-1234', 'contacto@mataderosprop.example', '{"corner": true}'),
('shop-haedo-01', 'Local Rivadavia 16200', 'Av. Rivadavia 16200', 'Haedo', 'kiosco', -34.6432, -58.6085, 'AVAILABLE',
 now() - interval '12 days', 45, '$520,000 / mes', NULL, '+54 11 4658-7788', 'consultas@haedonegocios.example', '{}'),
('shop-mataderos-02', 'Local Directorio 5400', 'Av. Directorio 5400', 'Mataderos', 'verduleria', -34.648, -58.495, 'AVAILABLE',
 now() - interval '20 days', 90, '$1,100,000 / mes', NULL, '+54 11 4687-5521', 'info@mataderosbrokers.example', '{"corner": false}'),
('shop-haedo-02', 'Local Estrada 400', 'Estrada 400', 'Haedo', 'almacen', -34.64, -58.601, 'UNDER_REVIEW',
 now() - interval '30 days', 60, '$650,000 / mes', NULL, '+54 11 4658-3090', 'ventas@haedopropiedades.example', '{}'),
('shop-mataderos-rented-01', 'Local Alberdi 6800', 'Av. Juan Bautista Alberdi 6800', 'Mataderos', 'cafeteria', -34.6525, -58.502, 'RENTED',
 now() - interval '40 days', 75, '$850,000 / mes', NULL, '+54 11 4687-1234', 'contacto@mataderosprop.example', '{}');

UPDATE shops SET rented_report_count = 3 WHERE id = 'shop-mataderos-rented-01';
UPDATE shops SET rented_report_count = 1 WHERE id = 'shop-haedo-02';

WITH mock_votes (shop_id, category, n) AS (
    VALUES
    ('shop-mataderos-01', 'cafeteria', 34), ('shop-mataderos-01', 'verduleria', 15), ('shop-mataderos-01', 'panaderia', 9),
    ('shop-haedo-01', 'kiosco', 3), ('shop-haedo-01', 'libreria', 2), ('shop-haedo-01', 'regaleria', 1),
    ('shop-mataderos-02', 'verduleria', 14), ('shop-mataderos-02', 'carniceria', 10),
    ('shop-haedo-02', 'almacen', 7), ('shop-haedo-02', 'kiosco', 4),
    ('shop-mataderos-rented-01', 'cafeteria', 34), ('shop-mataderos-rented-01', 'verduleria', 15), ('shop-mataderos-rented-01', 'panaderia', 9)
), expanded AS (
    SELECT m.shop_id, m.category, i, now() - (random() * interval '25 days') AS ts
      FROM mock_votes m, LATERAL generate_series(1, m.n) AS i
)
INSERT INTO votes (shop_id, category, frequency, scanned_at, fingerprint_hash, client_token_hash, normalization_status, created_at)
SELECT shop_id, category,
       (ARRAY['DAILY', 'WEEKLY', 'OCCASIONAL', 'DAILY', NULL])[1 + i % 5],
       ts,
       md5(shop_id || category || i),
       md5('token' || shop_id || category || i),
       'classified',
       ts
  FROM expanded;

INSERT INTO shops (id, name, address_text, neighborhood, top_category, latitude, longitude, status,
                   last_verified_at, surface_area_m2, rental_price_text, attributes)
SELECT 'shop-gen-' || lpad(g::text, 5, '0'),
       'Local generado ' || g,
       'Calle Generada ' || (100 + g % 9000),
       (ARRAY['Mataderos', 'Haedo', 'Liniers', 'Flores', 'Caballito', 'Ramos Mejia', 'Moron', 'Ciudadela'])[1 + g % 8],
       (ARRAY['cafeteria', 'verduleria', 'panaderia', 'kiosco', 'almacen', 'carniceria', 'libreria', 'ferreteria'])[1 + g % 8],
       -34.75 + random() * 0.25,
       -58.70 + random() * 0.35,
       (ARRAY['AVAILABLE', 'AVAILABLE', 'AVAILABLE', 'AVAILABLE', 'UNDER_REVIEW', 'RENTED'])[1 + g % 6],
       now() - (random() * interval '90 days'),
       30 + g % 120,
       '$' || (300000 + (g % 50) * 20000) || ' / mes',
       jsonb_build_object('corner', g % 3 = 0)
  FROM generate_series(1, 10000) AS g;

INSERT INTO votes (shop_id, category, frequency, scanned_at, fingerprint_hash, client_token_hash, normalization_status, created_at)
SELECT 'shop-gen-' || lpad((1 + (g * 7919) % 10000)::text, 5, '0'),
       (ARRAY['cafeteria', 'verduleria', 'panaderia', 'kiosco', 'almacen', 'carniceria', 'libreria', 'ferreteria'])[1 + g % 8],
       (ARRAY['DAILY', 'WEEKLY', 'OCCASIONAL', NULL])[1 + g % 4],
       ts,
       md5('gen-fp-' || g),
       md5('gen-token-' || g),
       'classified',
       ts
  FROM (SELECT g, now() - (random() * interval '60 days') AS ts FROM generate_series(1, 30000) AS g) t;

INSERT INTO votes (shop_id, category, category_raw, frequency, scanned_at, fingerprint_hash, client_token_hash, normalization_status, created_at)
SELECT 'shop-mataderos-01', NULL, 'gimnasio ' || g, NULL, now() - interval '1 day',
       md5('pending-fp-' || g), md5('pending-token-' || g), 'pending', now() - interval '1 day'
  FROM generate_series(1, 3) AS g;

ANALYZE shops;
ANALYZE votes;
