CREATE TABLE votes (
    id                    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    shop_id               text NOT NULL REFERENCES shops (id) ON DELETE CASCADE,
    category              text,
    category_raw          text,
    frequency             text,
    scanned_at            timestamptz NOT NULL,
    day_of_week           text GENERATED ALWAYS AS (
                              CASE extract(isodow FROM (scanned_at AT TIME ZONE 'America/Argentina/Buenos_Aires'))::int
                                  WHEN 1 THEN 'MONDAY'
                                  WHEN 2 THEN 'TUESDAY'
                                  WHEN 3 THEN 'WEDNESDAY'
                                  WHEN 4 THEN 'THURSDAY'
                                  WHEN 5 THEN 'FRIDAY'
                                  WHEN 6 THEN 'SATURDAY'
                                  ELSE 'SUNDAY'
                              END) STORED,
    hour_bucket           text GENERATED ALWAYS AS (
                              CASE
                                  WHEN extract(hour FROM (scanned_at AT TIME ZONE 'America/Argentina/Buenos_Aires')) < 12 THEN 'MORNING'
                                  WHEN extract(hour FROM (scanned_at AT TIME ZONE 'America/Argentina/Buenos_Aires')) < 19 THEN 'AFTERNOON'
                                  ELSE 'NIGHT'
                              END) STORED,
    fingerprint_hash      text NOT NULL,
    client_token_hash     text NOT NULL,
    normalization_status  text NOT NULL DEFAULT 'pending',
    created_at            timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT votes_frequency_chk CHECK (frequency IS NULL OR frequency IN ('DAILY', 'WEEKLY', 'OCCASIONAL')),
    CONSTRAINT votes_normalization_status_chk CHECK (normalization_status IN ('pending', 'classified', 'needs_review')),
    CONSTRAINT votes_category_present_chk CHECK (category IS NOT NULL OR category_raw IS NOT NULL),
    CONSTRAINT votes_classified_has_category_chk CHECK (normalization_status <> 'classified' OR category IS NOT NULL)
);

CREATE INDEX votes_shop_created_idx ON votes (shop_id, created_at);
CREATE INDEX votes_dedup_idx ON votes (shop_id, fingerprint_hash, created_at DESC);
CREATE INDEX votes_client_token_idx ON votes (shop_id, client_token_hash, created_at DESC);
CREATE INDEX votes_normalization_queue_idx ON votes (created_at) WHERE normalization_status <> 'classified';

COMMENT ON COLUMN votes.category IS 'Normalized category slug; NULL until BE-03 classifies a free-text answer';
COMMENT ON COLUMN votes.category_raw IS 'Free text typed under the Other option';
COMMENT ON COLUMN votes.day_of_week IS 'Generated from scanned_at in America/Argentina/Buenos_Aires';
COMMENT ON COLUMN votes.hour_bucket IS 'MORNING before 12, AFTERNOON before 19, NIGHT otherwise';
