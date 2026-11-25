#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# reset_db.sh — Destruye y recrea el volumen de la base de datos
# ejecutando todas las migraciones desde cero.
# USO: ./scripts/reset_db.sh
# ─────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$(dirname "$SCRIPT_DIR")"

cd "$SERVER_DIR"

echo "⚠️  Esto eliminará TODOS los datos de la base de datos local."
read -r -p "¿Continuar? (s/N): " confirm
[[ "$confirm" =~ ^[sS]$ ]] || { echo "Cancelado."; exit 0; }

echo "🛑 Deteniendo contenedores..."
docker compose down -v --remove-orphans

echo "🔄 Levantando DB y ejecutando migraciones..."
docker compose up --build --wait db flyway

echo "✅ Base de datos recreada exitosamente."
echo "   Podés conectarte con: psql postgresql://vidrierapp:vidrierapp_secret@localhost:5432/vidrierapp"
