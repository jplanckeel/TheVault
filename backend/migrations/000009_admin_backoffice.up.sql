-- Admin role on user accounts (replaces the shared ADMIN_PASSWORD).
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- Items corrected by an admin keep their catalogue fields on later syncs.
ALTER TABLE items ADD COLUMN IF NOT EXISTS admin_edited BOOLEAN NOT NULL DEFAULT FALSE;

-- External IDs of merged items, redirected to the item that was kept.
CREATE TABLE IF NOT EXISTS item_aliases (
    external_id TEXT PRIMARY KEY,
    item_id     UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- External IDs deleted by an admin: later syncs of these IDs are rejected.
CREATE TABLE IF NOT EXISTS item_tombstones (
    external_id TEXT PRIMARY KEY,
    title       TEXT NOT NULL DEFAULT '',
    deleted_by  TEXT NOT NULL DEFAULT '',
    deleted_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Audit log of catalogue writes: mobile / web syncs and admin corrections.
CREATE TABLE IF NOT EXISTS sync_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source      TEXT NOT NULL,              -- mobile | web | admin
    action      TEXT NOT NULL,              -- created | updated | rejected | edited | deleted | merged
    item_id     UUID REFERENCES items(id) ON DELETE SET NULL,
    external_id TEXT NOT NULL DEFAULT '',
    title       TEXT NOT NULL DEFAULT '',
    actor       TEXT NOT NULL DEFAULT '',
    detail      TEXT NOT NULL DEFAULT '',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sync_events_created_at ON sync_events(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sync_events_item_id ON sync_events(item_id);
