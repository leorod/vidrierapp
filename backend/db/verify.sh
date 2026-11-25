#!/usr/bin/env bash
set -u
cd "$(dirname "$0")/.."

PASS=0
FAIL=0

run_sql() {
    printf '%s\n' "$1" | docker compose exec -T db sh -c 'psql -X -q -t -A -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' 2>&1
}

check() {
    local name="$1" expected="$2" actual="$3"
    if [ "$expected" = "$actual" ]; then
        printf 'PASS  %s\n' "$name"
        PASS=$((PASS + 1))
    else
        printf 'FAIL  %s (expected [%s], got [%s])\n' "$name" "$expected" "$actual"
        FAIL=$((FAIL + 1))
    fi
}

expect_error() {
    local name="$1" sql="$2" pattern="$3" out
    out=$(run_sql "$sql")
    if [ $? -ne 0 ] && printf '%s' "$out" | grep -q "$pattern"; then
        printf 'PASS  %s\n' "$name"
        PASS=$((PASS + 1))
    else
        printf 'FAIL  %s (output: %s)\n' "$name" "$out"
        FAIL=$((FAIL + 1))
    fi
}

check "postgis extension present" "postgis" "$(run_sql "SELECT extname FROM pg_extension WHERE extname='postgis'")"

for t in shops votes device_trials pass_purchases; do
    check "table $t exists" "$t" "$(run_sql "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_name='$t'")"
done

check "shops columns" "10" "$(run_sql "SELECT count(*) FROM information_schema.columns WHERE table_name='shops' AND column_name IN ('id','name','address_text','latitude','longitude','geom','status','rented_report_count','last_verified_at','attributes')")"
check "shops.attributes is jsonb" "jsonb" "$(run_sql "SELECT data_type FROM information_schema.columns WHERE table_name='shops' AND column_name='attributes'")"
check "shops.geom is generated" "ALWAYS" "$(run_sql "SELECT is_generated FROM information_schema.columns WHERE table_name='shops' AND column_name='geom'")"
check "votes columns" "11" "$(run_sql "SELECT count(*) FROM information_schema.columns WHERE table_name='votes' AND column_name IN ('shop_id','category','category_raw','frequency','day_of_week','hour_bucket','fingerprint_hash','client_token_hash','scanned_at','created_at','normalization_status')")"
check "votes.shop_id foreign key" "1" "$(run_sql "SELECT count(*) FROM information_schema.table_constraints WHERE table_name='votes' AND constraint_type='FOREIGN KEY'")"
check "device_trials primary key on device hash" "device_id_hash" "$(run_sql "SELECT a.attname FROM pg_index i JOIN pg_attribute a ON a.attrelid=i.indrelid AND a.attnum=ANY(i.indkey) WHERE i.indrelid='device_trials'::regclass AND i.indisprimary")"
check "gist index on shops.geom" "shops_geom_gix" "$(run_sql "SELECT indexname FROM pg_indexes WHERE tablename='shops' AND indexdef ILIKE '%USING gist%'")"
check "indexes present" "4" "$(run_sql "SELECT count(*) FROM pg_indexes WHERE indexname IN ('shops_status_idx','votes_shop_created_idx','votes_dedup_idx','shops_last_verified_idx')")"
check "seed: shops loaded >= 10000" "t" "$(run_sql "SELECT count(*) >= 10000 FROM shops")"
check "seed: android mock shops present" "5" "$(run_sql "SELECT count(*) FROM shops WHERE id IN ('shop-mataderos-01','shop-haedo-01','shop-mataderos-02','shop-haedo-02','shop-mataderos-rented-01')")"
check "geom synced with lat/lng" "t" "$(run_sql "SELECT ST_X(geom::geometry)=longitude AND ST_Y(geom::geometry)=latitude FROM shops WHERE id='shop-mataderos-01'")"

expect_error "invalid shop status rejected" "INSERT INTO shops (id,name,address_text,neighborhood,latitude,longitude,status) VALUES ('bad-status','x','x','x',-34.6,-58.4,'FOO')" "shops_status_chk"
expect_error "invalid vote frequency rejected" "INSERT INTO votes (shop_id,category,frequency,scanned_at,fingerprint_hash,client_token_hash) VALUES ('shop-haedo-01','kiosco','MONTHLY',now(),'a','b')" "votes_frequency_chk"
expect_error "invalid normalization_status rejected" "INSERT INTO votes (shop_id,category,scanned_at,fingerprint_hash,client_token_hash,normalization_status) VALUES ('shop-haedo-01','kiosco',now(),'a','b','done')" "votes_normalization_status_chk"
expect_error "vote for unknown shop rejected" "INSERT INTO votes (shop_id,category,scanned_at,fingerprint_hash,client_token_hash) VALUES ('nope','kiosco',now(),'a','b')" "violates foreign key"

run_sql "DELETE FROM device_trials WHERE device_id_hash='verify-device'" >/dev/null
run_sql "INSERT INTO device_trials (device_id_hash, started_at, expires_at) VALUES ('verify-device', now(), now() + interval '3 days')" >/dev/null
check "first device trial accepted" "1" "$(run_sql "SELECT count(*) FROM device_trials WHERE device_id_hash='verify-device'")"
expect_error "second trial for same device rejected" "INSERT INTO device_trials (device_id_hash, started_at, expires_at) VALUES ('verify-device', now(), now() + interval '3 days')" "duplicate key"
expect_error "trial longer than 3 days rejected" "INSERT INTO device_trials (device_id_hash, started_at, expires_at) VALUES ('verify-device-2', now(), now() + interval '10 days')" "device_trials_length_chk"
run_sql "DELETE FROM device_trials WHERE device_id_hash='verify-device'" >/dev/null

RR=$(run_sql "BEGIN;
SELECT rented_report_count || ',' || status FROM report_rented('shop-haedo-01');
SELECT rented_report_count || ',' || status FROM report_rented('shop-haedo-01');
SELECT rented_report_count || ',' || status FROM report_rented('shop-haedo-01');
ROLLBACK;" | grep -E '^[0-9],' | paste -sd' ' -)
check "report_rented: 3rd report flips to UNDER_REVIEW" "1,AVAILABLE 2,AVAILABLE 3,UNDER_REVIEW" "$RR"

check "RENTED shop absent from listable_shops" "0" "$(run_sql "SELECT count(*) FROM listable_shops WHERE id='shop-mataderos-rented-01'")"
check "RENTED shop present in shops table" "1" "$(run_sql "SELECT count(*) FROM shops WHERE id='shop-mataderos-rented-01'")"
check "no RENTED rows in listable_shops" "0" "$(run_sql "SELECT count(*) FROM listable_shops WHERE status='RENTED'")"
check "zone_summary counts votes of RENTED shops" "t" "$(run_sql "SELECT (SELECT sum(vote_count) FROM zone_summary WHERE zone='Mataderos') = (SELECT count(*) FROM votes v JOIN shops s ON s.id=v.shop_id WHERE s.neighborhood='Mataderos' AND v.category IS NOT NULL)")"
check "zone_summary rented votes > 0" "t" "$(run_sql "SELECT sum(rented_shop_vote_count) > 0 FROM zone_summary WHERE zone='Mataderos'")"
check "zone_summary includes votes the listing would drop" "t" "$(run_sql "SELECT (SELECT sum(vote_count) FROM zone_summary WHERE zone='Mataderos') > (SELECT count(*) FROM votes v JOIN listable_shops s ON s.id=v.shop_id WHERE s.neighborhood='Mataderos' AND v.category IS NOT NULL)")"

CV=$(run_sql "
BEGIN;
INSERT INTO votes (shop_id,category,scanned_at,fingerprint_hash,client_token_hash,normalization_status,created_at)
VALUES ('shop-haedo-01','kiosco','2026-01-01 12:00+00','fp-test','tok-test','classified','2026-01-01 12:00+00');
SELECT can_vote('shop-haedo-01','fp-test','2026-01-01 12:00+00');
SELECT can_vote('shop-haedo-01','fp-test','2026-01-31 11:59:59+00');
SELECT can_vote('shop-haedo-01','fp-test','2026-01-31 12:00:00+00');
SELECT can_vote('shop-haedo-01','fp-test','2026-01-31 12:00:01+00');
SELECT can_vote('shop-haedo-01','fp-other','2026-01-01 12:00+00');
SELECT can_vote('shop-mataderos-rented-01','fp-new');
SELECT can_vote('missing-shop','fp-new');
ROLLBACK;" | grep -E '^(t|f)$' | paste -sd, -)
check "can_vote 30 day boundaries" "f,f,t,t,t,f,f" "$CV"

check "expire_stale_shops flips only stale AVAILABLE" "t" "$(run_sql "BEGIN; SELECT expire_stale_shops(now() + interval '365 days') > 0; ROLLBACK;" | head -1)"

BBOX="SELECT id FROM listable_shops WHERE geom && ST_MakeEnvelope(-58.52,-34.67,-58.48,-34.63,4326)::geography"
DWITHIN="SELECT id FROM listable_shops WHERE ST_DWithin(geom, ST_SetSRID(ST_MakePoint(-58.50,-34.65),4326)::geography, 1500)"
for pair in "bbox|$BBOX" "dwithin|$DWITHIN"; do
    label="${pair%%|*}"
    query="${pair#*|}"
    plan=$(run_sql "SELECT count(*) FROM ($query) w; EXPLAIN (ANALYZE, BUFFERS) $query" | tail -n +2)
    printf '%s\n' "$plan" | sed 's/^/    /'
    ms=$(printf '%s\n' "$plan" | awk '/Execution Time/ {print $3}' | tail -1)
    total=$(run_sql "SELECT count(*) FROM shops")
    rows=$(run_sql "SELECT count(*) FROM ($query) q")
    check "$label: >= 10000 shops seeded ($total) and rows returned ($rows)" "t" "$([ "$total" -ge 10000 ] && [ "$rows" -gt 0 ] && echo t || echo f)"
    check "$label: uses GiST index" "t" "$(printf '%s' "$plan" | grep -q 'shops_geom_gix' && echo t || echo f)"
    check "$label: no sequential scan" "t" "$(printf '%s' "$plan" | grep -q 'Seq Scan on shops' && echo f || echo t)"
    check "$label: execution ${ms} ms < 50 ms" "t" "$(awk -v m="$ms" 'BEGIN{print (m < 50) ? "t" : "f"}')"
done

printf '\n%d passed, %d failed\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
