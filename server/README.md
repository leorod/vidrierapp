# VidrierApp — Backend (BE-01)

Infraestructura de base de datos para VidrierApp: **PostgreSQL 16 + PostGIS 3.4** con migraciones versionadas via **Flyway**.

## Estructura

```
server/
├── docker-compose.yml          # Orquesta DB + Flyway + Adminer
├── .env.example                # Plantilla de variables de entorno
├── .gitignore
├── db/
│   ├── init/
│   │   └── 00_extensions.sql   # Extensiones (postgis, uuid-ossp, pg_trgm)
│   └── migrations/
│       ├── V1__create_shops.sql          # Tabla shops + índices espaciales
│       ├── V2__create_votes.sql          # Tabla votes + anti-duplicados
│       ├── V3__create_device_trials.sql  # Tabla device_trials (trial 3 días)
│       ├── V4__spatial_indexes_and_views.sql  # Vistas, funciones bbox
│       └── V5__seed_categories.sql       # Catálogo de rubros + alias
└── scripts/
    ├── reset_db.sh             # Destruye y recrea la DB desde cero
    └── validate_spatial.sh     # Verifica < 50 ms en query bounding-box
```

## Quickstart

```bash
# 1. Configurar variables de entorno
cp .env.example .env

# 2. Levantar DB y ejecutar migraciones
docker compose up db flyway

# 3. Verificar estado
docker compose ps

# 4. Abrir Adminer (UI web)
open http://localhost:8080
# Sistema: PostgreSQL | Server: db | Usuario/Contraseña: vidrierapp | DB: vidrierapp

# 5. Validar consulta espacial < 50 ms
chmod +x scripts/validate_spatial.sh
./scripts/validate_spatial.sh
```

## Modelo de datos

### `shops` — Locales comerciales

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` PK | Clave primaria propia (ADR §4.3) |
| `address_text` | `TEXT` | Dirección ingresada por el admin |
| `place_id` | `TEXT` | ID de Google; opcional, no único |
| `location` | `GEOGRAPHY(Point,4326)` | Coordenada elegida manualmente |
| `location_override_used` | `BOOLEAN` | `true` si se confirmó ubicación lejana |
| `status` | `shop_status` | `AVAILABLE \| UNDER_REVIEW \| RENTED` |
| `last_verified_at` | `TIMESTAMPTZ` | Revalidación; caduca a 60 días |
| `rented_report_count` | `INT` | Contador de reportes de alquiler |
| `attributes` | `JSONB` | Superficie, precio, inmobiliaria, etc. |
| `created_by / updated_by` | `TEXT` | Auditoría |

### `votes` — Votos de vecinos

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` PK | — |
| `shop_id` | `UUID` FK | Referencia a `shops` |
| `category` | `TEXT` | Rubro normalizado (slug canónico) |
| `raw_category_input` | `TEXT` | Texto libre original si eligió "Otro" |
| `category_status` | `CLASSIFIED \| UNCLASSIFIED` | Estado del pipeline BE-03 |
| `frequency` | `visit_frequency` | `DAILY \| WEEKLY \| OCCASIONAL` (nullable) |
| `day_of_week` | `SMALLINT` | 0 (Dom) – 6 (Sáb) |
| `hour_bucket` | `SMALLINT` | Hora de inicio del bloque de 2 hs |
| `fingerprint_hash` | `TEXT` | SHA-256 de huella técnica anónima |
| `browser_token` | `TEXT` | Token de localStorage del vecino |

### `device_trials` — Trial y pase por dispositivo

| Columna | Tipo | Descripción |
|---|---|---|
| `device_id` | `TEXT` UNIQUE | Hardware ID de la app Android |
| `status` | `trial_status` | `ACTIVE_TRIAL \| TRIAL_EXPIRED \| PASS_ACTIVE \| PASS_EXPIRED` |
| `trial_started_at` | `TIMESTAMPTZ` | Inicio del trial |
| `trial_expires_at` | `TIMESTAMPTZ` GENERATED | `trial_started_at + 3 days` |
| `pass_activated_at` | `TIMESTAMPTZ` | Nulo hasta compra |
| `pass_expires_at` | `TIMESTAMPTZ` | `pass_activated_at + 30 days` |

## Consultas clave

### Locales en bounding-box (< 50 ms, BE-01)
```sql
-- Mataderos / Buenos Aires
SELECT * FROM fn_shops_in_bbox(-58.55, -34.70, -58.45, -34.60, 'AVAILABLE');
```

### Verificar duplicado de voto (BE-02)
```sql
SELECT fn_vote_is_duplicate('<fingerprint_hash>', '<shop_uuid>');
```

### Métricas de un local (RF03)
```sql
SELECT * FROM mv_shop_vote_summary WHERE shop_id = '<uuid>';
```

### Distribución horaria (DET-01)
```sql
SELECT day_of_week, hour_bucket, vote_count
FROM mv_shop_hourly_distribution
WHERE shop_id = '<uuid>'
ORDER BY day_of_week, hour_bucket;
```

### Refrescar vistas materializadas (tras carga masiva de votos)
```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_shop_vote_summary;
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_shop_hourly_distribution;
```

## Checklist BE-01

- [x] PostgreSQL con extensión `postgis` habilitada
- [x] Tabla `shops` con geometría `GEOGRAPHY(Point,4326)`, `status`, `last_verified_at`, `attributes JSONB`
- [x] Tabla `votes` con `category`, `frequency` (nullable), `day_of_week`, `hour_bucket`, `fingerprint_hash`, `created_at`
- [x] Tabla `device_trials` con `device_id` único para trial de 3 días
- [x] Índice GIST espacial + función `fn_shops_in_bbox` para validar < 50 ms
