CREATE TABLE shops (
    id                      text PRIMARY KEY,
    name                    text NOT NULL,
    address_text            text NOT NULL,
    neighborhood            text NOT NULL,
    top_category            text,
    latitude                double precision NOT NULL,
    longitude               double precision NOT NULL,
    geom                    geography(Point, 4326) GENERATED ALWAYS AS
                                (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography) STORED,
    status                  text NOT NULL DEFAULT 'AVAILABLE',
    rented_report_count     integer NOT NULL DEFAULT 0,
    last_verified_at        timestamptz NOT NULL DEFAULT now(),
    surface_area_m2         integer,
    rental_price_text       text,
    contact_agency          text,
    contact_phone           text,
    contact_email           text,
    place_id                text,
    location_override_used  boolean NOT NULL DEFAULT false,
    attributes              jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_by              text,
    updated_by              text,
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT shops_status_chk CHECK (status IN ('AVAILABLE', 'UNDER_REVIEW', 'RENTED')),
    CONSTRAINT shops_latitude_chk CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT shops_longitude_chk CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT shops_report_count_chk CHECK (rented_report_count >= 0),
    CONSTRAINT shops_surface_chk CHECK (surface_area_m2 IS NULL OR surface_area_m2 > 0),
    CONSTRAINT shops_attributes_object_chk CHECK (jsonb_typeof(attributes) = 'object')
);

CREATE INDEX shops_geom_gix ON shops USING gist (geom);
CREATE INDEX shops_status_idx ON shops (status);
CREATE INDEX shops_neighborhood_idx ON shops (neighborhood);
CREATE INDEX shops_last_verified_idx ON shops (last_verified_at) WHERE status <> 'RENTED';
CREATE INDEX shops_attributes_gin ON shops USING gin (attributes jsonb_path_ops);

CREATE FUNCTION shops_touch_updated_at() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER shops_touch_updated_at_trg
    BEFORE UPDATE ON shops
    FOR EACH ROW EXECUTE FUNCTION shops_touch_updated_at();

CREATE FUNCTION shops_apply_report_threshold() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.rented_report_count >= 3 AND NEW.status = 'AVAILABLE' THEN
        NEW.status := 'UNDER_REVIEW';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER shops_apply_report_threshold_trg
    BEFORE INSERT OR UPDATE OF rented_report_count ON shops
    FOR EACH ROW EXECUTE FUNCTION shops_apply_report_threshold();

COMMENT ON COLUMN shops.geom IS 'Generated from longitude/latitude, always in sync';
COMMENT ON COLUMN shops.rented_report_count IS 'REP-01 reports; 3 or more moves AVAILABLE to UNDER_REVIEW';
COMMENT ON COLUMN shops.last_verified_at IS 'Last revalidation; publications expire after 60 days without it';
COMMENT ON COLUMN shops.top_category IS 'Normalized category slug of the most voted category';
