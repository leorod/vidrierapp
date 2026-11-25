-- ─────────────────────────────────────────────────────────────
-- V5 — Tabla de catálogo de rubros y datos de semilla
-- Ref: BE-03 (pipeline de normalización), WEB-01
-- ─────────────────────────────────────────────────────────────

-- ── Catálogo oficial de rubros ─────────────────────────────────
-- Usado por el pipeline determinístico (BE-03) como tabla de verdad.

CREATE TABLE IF NOT EXISTS categories (
    id          SERIAL      PRIMARY KEY,
    -- Clave canónica en snake_case (valor que se persiste en votes.category)
    slug        TEXT        NOT NULL UNIQUE,
    -- Etiqueta legible en español para mostrar en UI
    label_es    TEXT        NOT NULL,
    -- Icono sugerido (nombre de Material Icon o emoji)
    icon        TEXT,
    -- Orden de aparición en el formulario de voto (WEB-01)
    sort_order  SMALLINT    NOT NULL DEFAULT 99,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories (slug);

-- ── Semilla: rubros populares ──────────────────────────────────

INSERT INTO categories (slug, label_es, icon, sort_order) VALUES
    ('cafeteria',         'Cafetería / Bar',         '☕', 1),
    ('verduleria',        'Verdulería / Frutería',   '🥦', 2),
    ('panaderia',         'Panadería / Confitería',  '🥐', 3),
    ('carniceria',        'Carnicería',              '🥩', 4),
    ('farmacia',          'Farmacia',                '💊', 5),
    ('kiosco',            'Kiosco / Almacén',        '🛒', 6),
    ('ropa',              'Indumentaria / Ropa',     '👗', 7),
    ('calzado',           'Calzado',                 '👟', 8),
    ('ferreteria',        'Ferretería / Materiales', '🔧', 9),
    ('peluqueria',        'Peluquería / Barbería',   '✂️', 10),
    ('restaurante',       'Restaurante / Comida',    '🍽️', 11),
    ('gym',               'Gimnasio / Fitness',      '💪', 12),
    ('electrodomesticos', 'Electrodomésticos',       '📺', 13),
    ('libreria',          'Librería / Papelería',    '📚', 14),
    ('lavanderia',        'Lavandería / Tintorería', '🫧', 15),
    ('other',             'Otro',                    '➕', 99)
ON CONFLICT (slug) DO NOTHING;

-- ── Tabla de alias para el pipeline (BE-03) ────────────────────
-- Mapea variantes tipográficas, plurales y errores comunes al slug canónico.

CREATE TABLE IF NOT EXISTS category_aliases (
    id              SERIAL  PRIMARY KEY,
    alias           TEXT    NOT NULL UNIQUE,  -- variante normalizada (sin tildes, lowercase)
    canonical_slug  TEXT    NOT NULL REFERENCES categories(slug) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_category_aliases_alias
    ON category_aliases (alias);

-- Índice GIN trigram para fuzzy matching por similitud (pg_trgm, BE-03)
CREATE INDEX IF NOT EXISTS idx_category_aliases_trgm
    ON category_aliases USING GIN (alias gin_trgm_ops);

-- ── Semilla de alias ───────────────────────────────────────────

INSERT INTO category_aliases (alias, canonical_slug) VALUES
    -- cafetería
    ('cafe',           'cafeteria'),
    ('cafeteria',      'cafeteria'),
    ('bar',            'cafeteria'),
    ('cafeteriaa',     'cafeteria'),
    -- verdulería
    ('verduleria',     'verduleria'),
    ('verduleris',     'verduleria'),
    ('fruteria',       'verduleria'),
    ('verduras',       'verduleria'),
    -- panadería
    ('panaderia',      'panaderia'),
    ('panadria',       'panaderia'),
    ('confiteria',     'panaderia'),
    ('panderia',       'panaderia'),
    -- carnicería
    ('carniceria',     'carniceria'),
    ('carniseria',     'carniceria'),
    -- farmacia
    ('farmacia',       'farmacia'),
    ('farmacias',      'farmacia'),
    -- kiosco
    ('kiosco',         'kiosco'),
    ('quiosco',        'kiosco'),
    ('almacen',        'kiosco'),
    ('kiosko',         'kiosco'),
    -- ropa
    ('ropa',           'ropa'),
    ('indumentaria',   'ropa'),
    ('boutique',       'ropa'),
    -- calzado
    ('calzado',        'calzado'),
    ('zapatos',        'calzado'),
    ('zapateria',      'calzado'),
    -- ferretería
    ('ferreteria',     'ferreteria'),
    ('ferretria',      'ferreteria'),
    ('materiales',     'ferreteria'),
    -- peluquería
    ('peluqueria',     'peluqueria'),
    ('barberia',       'peluqueria'),
    ('peluquetia',     'peluqueria'),
    -- restaurante
    ('restaurante',    'restaurante'),
    ('restaurant',     'restaurante'),
    ('comida',         'restaurante'),
    ('rotiseria',      'restaurante'),
    -- gimnasio
    ('gym',            'gym'),
    ('gimnasio',       'gym'),
    ('gim',            'gym'),
    ('fitness',        'gym'),
    -- electrodomésticos
    ('electrodomesticos', 'electrodomesticos'),
    ('electro',        'electrodomesticos'),
    ('electronica',    'electrodomesticos'),
    -- librería
    ('libreria',       'libreria'),
    ('papeleria',      'libreria'),
    -- lavandería
    ('lavanderia',     'lavanderia'),
    ('tintoreria',     'lavanderia'),
    ('lavado',         'lavanderia')
ON CONFLICT (alias) DO NOTHING;

COMMENT ON TABLE categories        IS 'Catálogo oficial de rubros. Slug es el valor canónico persistido en votes.category.';
COMMENT ON TABLE category_aliases  IS 'Variantes tipográficas y sinónimos del catálogo. Usados por el pipeline de normalización (BE-03).';
