CREATE FUNCTION can_vote(p_shop_id text, p_fingerprint_hash text, p_now timestamptz DEFAULT now())
RETURNS boolean
LANGUAGE sql STABLE AS $$
    SELECT EXISTS (SELECT 1 FROM shops s WHERE s.id = p_shop_id AND s.status <> 'RENTED')
       AND NOT EXISTS (
            SELECT 1
              FROM votes v
             WHERE v.shop_id = p_shop_id
               AND v.fingerprint_hash = p_fingerprint_hash
               AND v.created_at > p_now - interval '30 days'
       );
$$;

CREATE FUNCTION expire_stale_shops(p_now timestamptz DEFAULT now())
RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE
    affected integer;
BEGIN
    UPDATE shops
       SET status = 'UNDER_REVIEW'
     WHERE status = 'AVAILABLE'
       AND last_verified_at < p_now - interval '60 days';
    GET DIAGNOSTICS affected = ROW_COUNT;
    RETURN affected;
END;
$$;

CREATE FUNCTION report_rented(p_shop_id text)
RETURNS shops
LANGUAGE sql AS $$
    UPDATE shops
       SET rented_report_count = rented_report_count + 1
     WHERE id = p_shop_id AND status <> 'RENTED'
    RETURNING *;
$$;

CREATE VIEW listable_shops AS
SELECT *
  FROM shops
 WHERE status <> 'RENTED';

CREATE VIEW zone_summary AS
SELECT s.neighborhood                                          AS zone,
       v.category                                              AS category,
       count(*)                                                AS vote_count,
       count(DISTINCT v.shop_id)                               AS shop_count,
       count(*) FILTER (WHERE v.frequency = 'DAILY')           AS daily_count,
       count(*) FILTER (WHERE v.frequency = 'WEEKLY')          AS weekly_count,
       count(*) FILTER (WHERE v.frequency = 'OCCASIONAL')      AS occasional_count,
       count(*) FILTER (WHERE s.status = 'RENTED')             AS rented_shop_vote_count
  FROM votes v
  JOIN shops s ON s.id = v.shop_id
 WHERE v.category IS NOT NULL
 GROUP BY s.neighborhood, v.category;

COMMENT ON VIEW zone_summary IS 'Includes votes of RENTED shops; listable_shops excludes them';
