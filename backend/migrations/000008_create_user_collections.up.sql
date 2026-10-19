CREATE TABLE IF NOT EXISTS user_collections (
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_id  UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    status   TEXT NOT NULL DEFAULT 'owned',
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, item_id)
);

CREATE INDEX idx_user_collections_user ON user_collections(user_id);
