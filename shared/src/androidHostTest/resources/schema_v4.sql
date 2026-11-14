CREATE TABLE game (
    id TEXT NOT NULL PRIMARY KEY,
    title TEXT NOT NULL,
    platform TEXT NOT NULL,
    genre TEXT NOT NULL,
    release_year INTEGER NOT NULL,
    region TEXT NOT NULL,
    condition_ TEXT NOT NULL,
    cover_url TEXT,
    status TEXT NOT NULL,
    purchase_price REAL,
    current_value REAL,
    notes TEXT,
    acquired_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    rating INTEGER,
    developer TEXT,
    publisher TEXT,
    game_modes TEXT,
    themes TEXT,
    is_collector_edition INTEGER NOT NULL DEFAULT 0,
    is_limited_edition INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX game_status ON game(status);
CREATE INDEX game_platform ON game(platform);
CREATE TABLE console (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    manufacturer TEXT NOT NULL,
    region TEXT NOT NULL,
    color TEXT NOT NULL,
    condition_ TEXT NOT NULL,
    production_year INTEGER NOT NULL,
    status TEXT NOT NULL,
    purchase_price REAL,
    current_value REAL,
    image_url TEXT,
    notes TEXT,
    acquired_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    is_collector_edition INTEGER NOT NULL DEFAULT 0,
    is_limited_edition INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX console_status ON console(status);
CREATE TABLE accessory (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    compatible_console TEXT NOT NULL,
    condition_ TEXT NOT NULL,
    status TEXT NOT NULL,
    purchase_price REAL,
    current_value REAL,
    image_url TEXT,
    notes TEXT,
    acquired_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    is_collector_edition INTEGER NOT NULL DEFAULT 0,
    is_limited_edition INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX accessory_status ON accessory(status);
CREATE TABLE price_quote (
    item_id TEXT NOT NULL,
    grade TEXT NOT NULL,
    median REAL NOT NULL,
    price_min REAL NOT NULL,
    price_max REAL NOT NULL,
    samples INTEGER NOT NULL,
    scraped_at TEXT NOT NULL,
    median_past REAL,
    PRIMARY KEY (item_id, grade)
);
CREATE TABLE value_snapshot (
    day TEXT NOT NULL PRIMARY KEY,
    total_value REAL NOT NULL,
    invested REAL NOT NULL
);
CREATE TABLE achievement_unlock (
    id TEXT NOT NULL PRIMARY KEY,
    unlocked_at TEXT NOT NULL,
    seen INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE app_setting (
    key TEXT NOT NULL PRIMARY KEY,
    value TEXT NOT NULL
);
CREATE TABLE sync_state (
    item_id TEXT NOT NULL PRIMARY KEY,
    synced_at TEXT NOT NULL
);
