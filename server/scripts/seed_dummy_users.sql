-- Credenciales de desarrollo: admin / Admin123!, usuario1 / User123!, usuario2 / User123!
-- Los valores password_hash son hashes PBKDF2-SHA256 precomputados, no contraseñas en texto plano.
INSERT INTO app_users (username, password_hash, role) VALUES
    ('admin', '210000:MDEyMzQ1Njc4OWFiY2RlZg==:0SpTl2Z22Mpkpqmt4RYAGat8yUJzt+xPeeFJ0eqLihc=', 'ADMIN'),
    ('usuario1', '210000:ZmVkY2JhOTg3NjU0MzIxMA==:MJJtkLPz3QLbhztv78NHzvjONWWrmMjEgSSV1sBhseM=', 'USER'),
    ('usuario2', '210000:MDEyMzQ1Njc4OWFiY2RlZg==:aUOCdygGUyDK9i71ej59W/3XMu5Fe3+Xc6QoQpGlJYo=', 'USER')
ON CONFLICT (username) DO NOTHING;
