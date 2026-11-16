-- When the IGDB enrichment last tried an item: items IGDB does not know are retried
-- weekly instead of on every run.
ALTER TABLE items ADD COLUMN IF NOT EXISTS igdb_checked_at TIMESTAMPTZ;
