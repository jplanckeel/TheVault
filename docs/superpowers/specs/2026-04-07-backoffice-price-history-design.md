# TheVaultBO — Back Office Price History Design

**Date:** 2026-04-07
**Status:** Approved

## Overview

TheVaultBO is a back office application that collects and stores price history for video games, consoles, and accessories from second-hand marketplaces (starting with eBay). It exposes a REST API consumed by the TheVault mobile app and provides a web frontend for administration.

---

## Architecture

Monolithic Go backend with modular internal packages. A single binary runs the HTTP API and the cron scheduler in the same process.

```
TheVaultBO/
├── Taskfile.yml
├── docker-compose.yml
├── .env.example
├── backend/
│   ├── cmd/server/main.go
│   ├── internal/
│   │   ├── api/        # HTTP handlers (chi router)
│   │   ├── scraper/    # eBay Browse API client
│   │   ├── scheduler/  # robfig/cron — scheduled jobs
│   │   ├── store/      # PostgreSQL access (pgx v5)
│   │   └── domain/     # Pure business entities
│   ├── migrations/     # SQL migration files (golang-migrate)
│   └── go.mod
└── frontend/
    ├── src/
    │   ├── pages/      # Items, ItemDetail, Jobs
    │   ├── components/ # PriceChart, ItemsTable, JobStatus
    │   ├── api/        # Typed fetch wrappers to /api/v1/*
    │   └── types/      # Item, PriceSnapshot, ScrapeJob
    ├── package.json
    └── vite.config.ts
```

**Main data flow:**
1. Mobile app `POST /api/v1/items` → upserts items into the back office
2. Scheduler triggers `scraper.Run(items)` on cron schedule (configurable) or on manual trigger from frontend
3. Scraper calls eBay Browse API, computes min/max/median per condition, stores a `price_snapshot`
4. Mobile app calls `GET /api/v1/items/:id/prices` to retrieve current market prices
5. Frontend displays the catalogue with latest prices and price history charts

---

## Data Model (PostgreSQL)

### `items`
Synced from the mobile app via API.

| Column       | Type         | Notes                              |
|--------------|--------------|------------------------------------|
| id           | UUID PK      |                                    |
| external_id  | TEXT UNIQUE  | ID from the mobile app             |
| type         | TEXT         | `game` \| `console` \| `accessory` |
| title        | TEXT         |                                    |
| platform     | TEXT         |                                    |
| region       | TEXT         |                                    |
| condition    | TEXT         | Overall item condition             |
| status       | TEXT         | `owned` \| `wishlist` \| `sold` \| `loaned` |
| cover_url    | TEXT         |                                    |
| created_at   | TIMESTAMPTZ  |                                    |
| updated_at   | TIMESTAMPTZ  |                                    |

### `price_snapshots`
One row per item per condition per scrape run.

| Column       | Type         | Notes                              |
|--------------|--------------|------------------------------------|
| id           | UUID PK      |                                    |
| item_id      | UUID FK      | → items(id)                        |
| source       | TEXT         | `ebay`                             |
| condition    | TEXT         | `loose` \| `cib` \| `new`          |
| price_min    | NUMERIC(10,2)|                                    |
| price_max    | NUMERIC(10,2)|                                    |
| price_median | NUMERIC(10,2)|                                    |
| sample_count | INT          | Number of listings analysed        |
| currency     | TEXT         | Default `EUR`                      |
| scraped_at   | TIMESTAMPTZ  |                                    |

### `scrape_jobs`
Tracks each scraping run.

| Column       | Type         | Notes                                     |
|--------------|--------------|-------------------------------------------|
| id           | UUID PK      |                                           |
| triggered_by | TEXT         | `cron` \| `manual`                        |
| status       | TEXT         | `pending` \| `running` \| `done` \| `failed` |
| items_total  | INT          |                                           |
| items_done   | INT          |                                           |
| error        | TEXT         | Set if status is `failed`                 |
| started_at   | TIMESTAMPTZ  |                                           |
| finished_at  | TIMESTAMPTZ  |                                           |

### `settings`
Key-value configuration store.

| Column | Type     | Notes                                |
|--------|----------|--------------------------------------|
| key    | TEXT PK  | e.g. `ebay_api_key`, `scrape_schedule` |
| value  | TEXT     |                                      |

**Key settings:**
- `ebay_marketplace` — default `EBAY_FR`
- `scrape_schedule` — cron expression, default `0 2 * * *`
- `scrape_sample_size` — listings per search, default `20`
- `ebay_oauth_token` + `ebay_token_expiry` — managed by the backend

---

## API Endpoints

All endpoints prefixed with `/api/v1`.

### Items
```
POST   /items              Upsert an item (called by mobile app)
GET    /items              List all items
GET    /items/:id          Get item detail
GET    /items/:id/prices   Get price history for an item
```

### Prices
```
GET    /prices/latest      Latest snapshot for all items
```

### Scraping
```
POST   /scrape                   Trigger a full manual scrape job (all items)
POST   /scrape/items/:id         Trigger a scrape for a single item
GET    /scrape/jobs              List all scrape jobs
GET    /scrape/jobs/:id          Get status of a specific job
```

### Health
```
GET    /healthz
```

---

## Scraper — eBay Browse API

**Endpoint:** `GET /buy/browse/v1/item_summary/search`

**Strategy per item (3 searches):**

| Condition | Query                                    | Filter              |
|-----------|------------------------------------------|---------------------|
| Loose     | `{title} {platform}` (excludes "complet")| `conditions:{USED}` |
| CIB       | `{title} {platform} complet`             | `conditions:{USED}` |
| New       | `{title} {platform}`                     | `conditions:{NEW}`  |

For each search: collect N listings (configurable, default 20) → compute min / max / median → store one `price_snapshot`.

**Error handling:**
- Rate limiting: exponential backoff between requests
- Item with zero results: snapshot stored with `sample_count=0`, no fatal error
- Failed job: status set to `failed` with error message; remaining items continue processing

---

## Backend Stack

| Concern      | Library                          | Version  |
|--------------|----------------------------------|----------|
| Router       | `go-chi/chi`                     | v5       |
| PostgreSQL   | `jackc/pgx`                      | v5       |
| Scheduler    | `robfig/cron`                    | v3       |
| Migrations   | `golang-migrate/migrate`         | v4       |
| Config       | `joho/godotenv`                  | latest   |
| Hot reload   | `cosmtrek/air`                   | latest   |

---

## Frontend Stack

| Concern      | Library                          |
|--------------|----------------------------------|
| Framework    | React 19 + TypeScript            |
| Build tool   | Vite (latest)                    |
| UI library   | `@semcore/ui` (Intergalactic)    |
| HTTP client  | Native fetch with typed wrappers |

**Pages:**

1. **Catalogue** (`/items`) — table of all items with latest Loose/CIB/New prices, freshness badge (green < 7d, orange < 30d, red > 30d), filters by type/platform/status
2. **Item detail** (`/items/:id`) — price history chart (Loose/CIB/New over time), snapshot table, "Scrape now" button
3. **Jobs** (`/jobs`) — list of scrape jobs with status/progress, "Run full scrape" button, auto-refresh every 5s when a job is running

---

## Local Development (Taskfile)

```yaml
tasks:
  dev:          # Starts PostgreSQL + backend + frontend concurrently
  dev:db:       # Starts PostgreSQL via Docker Compose only
  dev:backend:  # go run with air hot reload
  dev:frontend: # vite dev server

  db:migrate:   # Run migrations up
  db:reset:     # Drop + recreate database + run migrations

  build:        # go build + vite build
  test:         # go test ./...
  lint:         # golangci-lint
```

**Prerequisites:** Docker, Go, Node.js, Task CLI
**Environment:** `.env` file at repo root (see `.env.example`)

**Key env vars:**
```
DATABASE_URL=postgres://postgres:postgres@localhost:5432/thevaultbo
EBAY_API_KEY=
PORT=8080
VITE_API_URL=http://localhost:8080
```
