# VidrierApp — Backend (BE-01, BE-02, BE-03)

API REST en **Ktor + Kotlin** y PostgreSQL 16 + PostGIS 3.4 con migraciones versionadas mediante Flyway.

La organización de capas y las reglas de dependencia están documentadas en [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Estructura

```
server/
├── docker-compose.yml          # Orquesta DB + Flyway + Adminer
├── .env.example                # Plantilla de variables de entorno
├── .gitignore
├── build.gradle.kts              # Módulo Ktor incluido en el Gradle raíz
├── src/main/kotlin/              # API REST, normalización y acceso JDBC
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

# 2. Levantar DB, migraciones y API Ktor
docker compose up --build

# 3. Verificar estado
docker compose ps

# 4. Abrir Adminer (UI web)
open http://localhost:8080
# Sistema: PostgreSQL | Server: db | Usuario/Contraseña: vidrierapp | DB: vidrierapp

# 5. Validar que la API esté lista
open http://localhost:8081/health

# 6. Validar consulta espacial < 50 ms
chmod +x scripts/validate_spatial.sh
./scripts/validate_spatial.sh
```

Para ejecutar la API sin Docker, desde la raíz del repositorio usar `./gradlew :server:run`. La API queda disponible en `http://localhost:8081`.

## API MVP

| Método | Endpoint | Propósito |
|---|---|---|
| `GET` | `/health` | Health check sin base de datos. |
| `POST` | `/api/v1/auth/login` | Inicia sesión con usuario/contraseña y devuelve un JWT con perfil `ADMIN` o `USER`. |
| `GET` | `/api/v1/shops?category=&lonMin=&latMin=&lonMax=&latMax=` | Locales disponibles para mapa y filtro opcional; requiere JWT. |
| `GET` | `/api/v1/shops/{id}` | Ficha del local. Enviar `X-Device-Id` para habilitar métricas con trial/pase vigente. |
| `POST` | `/api/v1/votes` | Voto anónimo. Requiere `X-Fingerprint-Hash`; acepta `X-Browser-Token`. |
| `POST` | `/api/v1/shops/{id}/report-rented` | Incrementa reportes y pasa a revisión al tercer reporte. |
| `POST` | `/api/v1/auth/trial` | Inicia o consulta el trial único por dispositivo. |
| `POST` | `/api/v1/payments/checkout` | Procesa un checkout simulado y activa el pase de 30 días solo si se aprueba. |
| `GET` | `/api/v1/zones/summary` | Reportería histórica agregada por barrio, incluidos locales alquilados. |
| `POST` | `/api/v1/admin/shops` | Alta de local; requiere JWT de perfil `ADMIN`. |
| `POST` | `/api/v1/admin/geo/resolve` | Resuelve `address` (o `placeId`) vía Google y entrega un centro transitorio para el pin manual. |
| `POST` | `/api/v1/admin/shops/{id}/status` | Actualiza estado/revalida un local; requiere token admin. |
| `POST` | `/api/v1/admin/maintenance/run` | Fuerza caducidad a 60 días y refresca agregados; requiere token admin. |
| `GET` | `/api/v1/admin/categories/unclassified` | Lista términos libres pendientes de curaduría. |
| `POST` | `/api/v1/admin/categories/aliases` | Crea o actualiza un alias de rubro. |
| `POST` | `/api/v1/admin/categories/reclassify` | Reclasifica votos pendientes para un alias y refresca agregados. |

El detalle retorna métricas únicamente cuando el dispositivo tiene acceso vigente y el local alcanzó 15 votos. El normalizador determinístico limpia texto libre, consulta alias del catálogo y aplica Levenshtein antes de registrar un voto como no clasificado.

Los votos requieren los headers `X-Fingerprint-Hash` y `X-Browser-Token`. El backend rechaza duplicados por token o huella durante 30 días y aplica un límite suave de 20 solicitudes por IP hasheada por hora para preservar el uso compartido de redes móviles. El mantenimiento (caducidad de locales y accesos) se ejecuta cada hora; los agregados de votos se refrescan cada 30 segundos. Ambos pueden ejecutarse explícitamente desde el endpoint admin.

Para el alta administrativa, primero llamar `POST /api/v1/admin/geo/resolve` con la `address` escrita (o un `placeId` de Google). Requiere `MAPS_API_KEY` con Geocoding API habilitada. El resultado contiene un `resolutionId` que se envía al crear el local junto al pin manual. El backend mantiene esa resolución solo 15 minutos, valida que el pin esté a 150 m o menos y nunca persiste las coordenadas recibidas de Google. Usar `confirmFarLocation: true` para la salida de emergencia documentada en el ADR; queda auditada como `locationOverrideUsed`.

## Pago simulado

`POST /api/v1/payments/checkout` recibe `deviceId` y `paymentToken`. No recibe ni almacena datos de tarjeta. Usar `mock_approve` para simular un cobro aprobado y activar el pase por 30 días; usar `mock_decline` para simular un rechazo. Cada intento queda auditado en `mock_payments`, incluyendo importe simulado y referencia del proveedor mock.

## Consola interna y web

Con `WEB_DIR` apuntando a la carpeta `web/` (el compose ya lo monta), el servidor entrega la página de voto en `/?localId=<id>` y la consola interna en `/admin/`. La consola permite dar de alta locales buscando la dirección y marcando el pin en un mapa (Leaflet + OpenStreetMap). Inicia sesión contra el mismo backend que Android y solo permite continuar a perfiles `ADMIN`; el JWT se conserva durante la pestaña actual. Configurar `JWT_SECRET` en `.env` antes de desplegar.

Las cuentas se guardan en `app_users` y las contraseñas se almacenan como hashes PBKDF2-SHA256. Tras ejecutar las migraciones, cargar las cuentas dummy de desarrollo con:

```bash
./scripts/seed_dummy_users.sh
```

El script es idempotente y crea estas cuentas: `admin` / `Admin123!` (perfil `ADMIN`), `usuario1` / `User123!` y `usuario2` / `User123!` (perfiles `USER`). No usar estas credenciales fuera de desarrollo.

## Conectar la app

```bash
./gradlew :app:assembleDebug -PUSE_MOCKS=false -PAPI_BASE_URL=http://10.0.2.2:8081/
```

Si el emulador no llega a `10.0.2.2`, redirigí el puerto y usá `localhost`:

```bash
adb reverse tcp:8081 tcp:8081
./gradlew :app:installDebug -PUSE_MOCKS=false -PAPI_BASE_URL=http://localhost:8081/
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
