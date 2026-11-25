#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# validate_spatial.sh — Valida que la consulta espacial por
# bounding-box responda en < 50 ms (criterio de aceptación BE-01).
# USO: ./scripts/validate_spatial.sh
# ─────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$(dirname "$SCRIPT_DIR")"

# Cargar variables de entorno si existe .env
if [[ -f "$SERVER_DIR/.env" ]]; then
    set -o allexport
    source "$SERVER_DIR/.env"
    set +o allexport
fi

PGHOST="${PGHOST:-localhost}"
PGPORT="${DB_HOST_PORT:-5432}"
PGUSER="${POSTGRES_USER:-vidrierapp}"
PGPASSWORD="${POSTGRES_PASSWORD:-vidrierapp_secret}"
PGDATABASE="${POSTGRES_DB:-vidrierapp}"

export PGPASSWORD

echo "🔍 Verificando extensiones PostGIS..."
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c \
    "SELECT name, default_version FROM pg_available_extensions WHERE name IN ('postgis','uuid-ossp','pg_trgm') ORDER BY name;"

echo ""
echo "⚡ Ejecutando consulta bounding-box de benchmark (Mataderos / Buenos Aires)..."
echo "   Bbox: lon [-58.55, -58.45] lat [-34.70, -34.60]"
echo ""

# EXPLAIN ANALYZE sobre la función de bounding-box
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c "
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT * FROM fn_shops_in_bbox(-58.55, -34.70, -58.45, -34.60, 'AVAILABLE');
"

echo ""
echo "📊 Contando filas en cada tabla..."
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c "
SELECT
    relname  AS tabla,
    n_live_tup AS filas_estimadas
FROM pg_stat_user_tables
WHERE relname IN ('shops','votes','device_trials','categories','category_aliases')
ORDER BY relname;
"

echo ""
echo "✅ Validación completada. Revisá el plan de ejecución para confirmar el uso del índice GIST."
