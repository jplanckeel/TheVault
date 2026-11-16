-- Previous backups of each user: a backup made from an empty phone no longer destroys
-- the only copy. The latest 10 are kept; user_backups stays the current one.
CREATE TABLE IF NOT EXISTS user_backup_versions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    payload    JSONB NOT NULL,
    item_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS user_backup_versions_user ON user_backup_versions (user_id, created_at DESC);

INSERT INTO user_backup_versions (user_id, payload, item_count, created_at)
SELECT user_id, payload, COALESCE(jsonb_array_length(payload -> 'items'), 0), updated_at FROM user_backups;
