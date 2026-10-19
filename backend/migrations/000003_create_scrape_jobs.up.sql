CREATE TABLE IF NOT EXISTS scrape_jobs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    triggered_by TEXT NOT NULL DEFAULT 'manual',
    status       TEXT NOT NULL DEFAULT 'pending',
    items_total  INT  NOT NULL DEFAULT 0,
    items_done   INT  NOT NULL DEFAULT 0,
    error        TEXT NOT NULL DEFAULT '',
    started_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at  TIMESTAMPTZ
);
