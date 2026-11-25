# VidrierApp backend: database layer (BE-01)

PostgreSQL 16 + PostGIS 3.4, schema managed with Flyway. The Ktor API (BE-02) is not part of this folder yet.

## Layout

| Path | Content |
|---|---|
| `docker-compose.yml` | `db` (postgis/postgis:16-3.4) and `flyway` (flyway/flyway:10.20, profile `tools`) |
| `db/migrations/V1..V5` | extension, `shops`, `votes`, `device_trials` + `pass_purchases`, functions and views |
| `db/seed/seed_shops.sql` | the 5 shops of `mock_shops.json`, their votes, 10,000 generated shops and 30,000 generated votes |
| `db/verify.sh` | assertions against the running container |
| `.env.example` | connection settings |

## Run

```sh
cd backend
cp .env.example .env
docker compose up -d db
docker compose run --rm flyway
docker compose exec -T db sh -c 'psql -q -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' < db/seed/seed_shops.sql
./db/verify.sh
docker compose down -v
```

`POSTGRES_PASSWORD=vidrierapp_dev_only` is a local development default only. Never reuse it outside a developer machine. `.env` is git-ignored.

Seeding truncates all tables, so it is for development only.

## Model

- `shops`: text id (`shop-mataderos-01` style), `latitude`/`longitude` and `geom geography(Point,4326)` as a generated stored column, so it cannot drift. `status` is `AVAILABLE | UNDER_REVIEW | RENTED`. `rented_report_count` reaching 3 moves `AVAILABLE` to `UNDER_REVIEW` through a trigger; `report_rented(shop_id)` increments it. `last_verified_at` drives the 60-day expiry (`expire_stale_shops()` moves stale `AVAILABLE` shops to `UNDER_REVIEW`). `attributes` is `jsonb`.
- `votes`: `category` is the normalized slug, `category_raw` holds the free text of "Other". `normalization_status` is `pending | classified | needs_review` for BE-03/BE-04. `day_of_week` and `hour_bucket` are generated from `scanned_at` in `America/Argentina/Buenos_Aires` (`MORNING` before 12, `AFTERNOON` before 19, `NIGHT` otherwise). `frequency` is nullable `DAILY | WEEKLY | OCCASIONAL`.
- `device_trials`: primary key on `device_id_hash`, so a second trial for the same device fails. `expires_at` must equal `started_at + 3 days`.
- `pass_purchases`: simulated 30-day pass (the technical document says there is no real payment gateway). `expires_at` must equal `purchased_at + 30 days`.

## Queries the API should use

Listing excludes rented shops, the zone summary does not:

```sql
SELECT * FROM listable_shops WHERE geom && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)::geography;
SELECT * FROM listable_shops WHERE ST_DWithin(geom, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :meters);
SELECT * FROM zone_summary WHERE zone = :zone;
```

`zone` is the shop `neighborhood`. `zone_summary` counts votes of `RENTED` shops (REP-01).

## Anti-duplicate rule (max 1 vote per 30 days per fingerprint and shop)

A unique index cannot express a rolling window. The API calls `can_vote(shop_id, fingerprint_hash [, now])` inside the transaction that inserts the vote. It returns `false` if the shop is unknown or `RENTED`, or if a vote with the same fingerprint exists newer than 30 days. A vote exactly 30 days old no longer blocks. The lookup uses `votes_dedup_idx (shop_id, fingerprint_hash, created_at DESC)`. Two concurrent first votes can both pass the check; BE-02 must serialize per `(shop_id, fingerprint_hash)`, for example with `pg_advisory_xact_lock(hashtextextended(shop_id || fingerprint_hash, 0))`.

## Performance note

The first geography `ST_DWithin` call in each new database session costs about 50 ms (library initialisation). Warm sessions answer in under 1 ms on 10,000 shops. Use a connection pool and run a warm-up query when a connection is created. `verify.sh` measures the warm call.
