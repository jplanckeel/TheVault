CREATE TABLE IF NOT EXISTS settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL DEFAULT ''
);

INSERT INTO settings (key, value) VALUES
    ('ebay_marketplace',   'EBAY_FR'),
    ('scrape_schedule',    '0 2 * * *'),
    ('scrape_sample_size', '20')
ON CONFLICT (key) DO NOTHING;
