-- Backup of the mobile collection, one per user (opaque JSON written by the app).
CREATE TABLE IF NOT EXISTS user_backups (
    user_id    UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    payload    JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Read-only public links to a user's collection or wishlist, built from the backup.
CREATE TABLE IF NOT EXISTS shares (
    token       TEXT PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scope       TEXT NOT NULL,              -- collection | wishlist
    title       TEXT NOT NULL DEFAULT '',
    hide_prices BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shares_user ON shares(user_id);
