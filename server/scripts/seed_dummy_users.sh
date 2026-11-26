#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$(dirname "$SCRIPT_DIR")"

cd "$SERVER_DIR"
docker compose exec -T db psql -U "${POSTGRES_USER:-vidrierapp}" -d "${POSTGRES_DB:-vidrierapp}" < "$SCRIPT_DIR/seed_dummy_users.sql"
