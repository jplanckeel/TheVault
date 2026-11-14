CREATE TABLE IF NOT EXISTS price_snapshots (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id      UUID NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    source       TEXT NOT NULL DEFAULT 'ebay',
    condition    TEXT NOT NULL,
    price_min    NUMERIC(10,2) NOT NULL DEFAULT 0,
    price_max    NUMERIC(10,2) NOT NULL DEFAULT 0,
    price_median NUMERIC(10,2) NOT NULL DEFAULT 0,
    sample_count INT           NOT NULL DEFAULT 0,
    currency     TEXT NOT NULL DEFAULT 'EUR',
    scraped_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_price_snapshots_item_id ON price_snapshots(item_id);
CREATE INDEX idx_price_snapshots_scraped_at ON price_snapshots(scraped_at DESC);
