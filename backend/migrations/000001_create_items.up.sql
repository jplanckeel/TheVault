CREATE TABLE IF NOT EXISTS items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id TEXT NOT NULL UNIQUE,
    type        TEXT NOT NULL,
    title       TEXT NOT NULL,
    platform    TEXT NOT NULL DEFAULT '',
    region      TEXT NOT NULL DEFAULT '',
    condition   TEXT NOT NULL DEFAULT '',
    status      TEXT NOT NULL DEFAULT 'owned',
    cover_url   TEXT NOT NULL DEFAULT '',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
