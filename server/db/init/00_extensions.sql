-- ─────────────────────────────────────────────────────────────
-- Script de inicialización ejecutado por PostgreSQL al crear la DB
-- por primera vez (docker-entrypoint-initdb.d).
-- Habilita las extensiones requeridas; Flyway se encarga del resto.
-- ─────────────────────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;    -- para búsquedas fuzzy de texto
