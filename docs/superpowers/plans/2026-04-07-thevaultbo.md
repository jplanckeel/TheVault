# TheVaultBO Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build TheVaultBO — a Go back office (REST API + cron scraper) and React admin frontend that syncs video game items from TheVault mobile app and collects eBay price history per condition (Loose/CIB/New).

**Architecture:** Single Go binary (`backend/`) running chi HTTP API + robfig/cron scheduler backed by PostgreSQL (pgx v5). Migrations auto-run at startup via embedded FS. React 19 + Vite + @semcore/ui frontend (`frontend/`) communicates via `/api/v1` REST. Taskfile orchestrates local dev with Docker Compose for PostgreSQL and Air for Go hot-reload.

**Tech Stack:** Go 1.24, chi v5, pgx v5, robfig/cron v3, golang-migrate v4, google/uuid v1.6, joho/godotenv v1.5; React 19, TypeScript 5, Vite 6, @semcore/ui, @semcore/d3-chart, react-router-dom v7

---

## File Map

### Root
- `Taskfile.yml` — dev/build/test/lint/db tasks
- `docker-compose.yml` — PostgreSQL service
- `.env.example` — env var template
- `.gitignore`

### Backend
- `backend/go.mod`
- `backend/.air.toml` — Air hot-reload config
- `backend/cmd/server/main.go` — entry point: loads config, wires deps, runs migrations, starts server
- `backend/internal/domain/models.go` — Item, PriceSnapshot, ScrapeJob, PriceStats structs
- `backend/internal/domain/stats.go` — ComputeStats(prices []float64) PriceStats
- `backend/internal/domain/stats_test.go` — unit tests for median/min/max
- `backend/internal/store/store.go` — ItemStore, PriceStore, JobStore, SettingsStore interfaces
- `backend/internal/store/db.go` — OpenDB() pgxpool.Pool + RunMigrations()
- `backend/internal/store/items.go` — PostgresItemStore
- `backend/internal/store/prices.go` — PostgresPriceStore
- `backend/internal/store/jobs.go` — PostgresJobStore
- `backend/internal/store/settings.go` — PostgresSettingsStore
- `backend/internal/api/router.go` — chi.Router with all routes + CORS + logger
- `backend/internal/api/handlers/items.go` — UpsertItem, ListItems, GetItem, GetItemPrices
- `backend/internal/api/handlers/items_test.go` — handler tests with mock stores
- `backend/internal/api/handlers/prices.go` — GetLatestPrices
- `backend/internal/api/handlers/scrape.go` — TriggerFullScrape, TriggerItemScrape, ListJobs, GetJob
- `backend/internal/scraper/ebay.go` — EbayClient: OAuth refresh + Browse API + price stats
- `backend/internal/scraper/ebay_test.go` — tests with mock HTTP server
- `backend/internal/scheduler/scheduler.go` — Scheduler wrapping robfig/cron
- `backend/migrations/000001_create_items.up.sql`
- `backend/migrations/000001_create_items.down.sql`
- `backend/migrations/000002_create_price_snapshots.up.sql`
- `backend/migrations/000002_create_price_snapshots.down.sql`
- `backend/migrations/000003_create_scrape_jobs.up.sql`
- `backend/migrations/000003_create_scrape_jobs.down.sql`
- `backend/migrations/000004_create_settings.up.sql`
- `backend/migrations/000004_create_settings.down.sql`

### Frontend
- `frontend/index.html`
- `frontend/package.json`
- `frontend/vite.config.ts`
- `frontend/tsconfig.json`
- `frontend/src/main.tsx`
- `frontend/src/App.tsx` — router setup with 3 routes
- `frontend/src/types/index.ts` — Item, PriceSnapshot, ScrapeJob, LatestPrice TS types
- `frontend/src/api/client.ts` — typed fetch wrappers for all endpoints
- `frontend/src/pages/Items.tsx` — catalogue: filters + table
- `frontend/src/pages/ItemDetail.tsx` — detail: price chart + snapshots table + scrape button
- `frontend/src/pages/Jobs.tsx` — jobs list + trigger + auto-refresh
- `frontend/src/components/ItemsTable.tsx` — @semcore DataTable with freshness badge
- `frontend/src/components/PriceChart.tsx` — @semcore/d3-chart line chart
- `frontend/src/components/JobStatus.tsx` — status badge + progress bar

---

## Task 1: Root Infrastructure

**Files:**
- Create: `Taskfile.yml`
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create: `.gitignore`

- [ ] **Step 1: Create `.gitignore`**

```gitignore
# env
.env

# Go
backend/bin/
backend/.air/
backend/vendor/

# Node
frontend/node_modules/
frontend/dist/

# OS
.DS_Store
```

- [ ] **Step 2: Create `docker-compose.yml`**

```yaml
services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: thevaultbo
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

- [ ] **Step 3: Create `.env.example`**

```dotenv
DATABASE_URL=postgres://postgres:postgres@localhost:5432/thevaultbo
PORT=8080
EBAY_CLIENT_ID=
EBAY_CLIENT_SECRET=
VITE_API_URL=http://localhost:8080
```

- [ ] **Step 4: Copy `.env.example` to `.env` and fill in your eBay credentials**

```bash
cp .env.example .env
```

- [ ] **Step 5: Create `Taskfile.yml`**

```yaml
version: '3'

dotenv: ['.env']

tasks:
  dev:
    desc: Start PostgreSQL, backend (hot-reload), and frontend dev server
    cmds:
      - docker compose up -d postgres
      - until docker compose exec -T postgres pg_isready -U postgres -q; do sleep 1; done
      - task dev:backend & task dev:frontend; wait

  dev:db:
    desc: Start PostgreSQL only and wait until ready
    cmds:
      - docker compose up -d postgres
      - until docker compose exec -T postgres pg_isready -U postgres -q; do sleep 1; done

  dev:backend:
    desc: Start backend with Air hot-reload
    dir: backend
    cmds:
      - air

  dev:frontend:
    desc: Start frontend Vite dev server
    dir: frontend
    cmds:
      - npm run dev

  db:reset:
    desc: Drop and recreate the database (server restarts will re-migrate)
    cmds:
      - docker compose exec -T postgres psql -U postgres -c "DROP DATABASE IF EXISTS thevaultbo"
      - docker compose exec -T postgres psql -U postgres -c "CREATE DATABASE thevaultbo"

  build:
    desc: Build backend binary and frontend bundle
    cmds:
      - cd backend && go build -o bin/server ./cmd/server
      - cd frontend && npm run build

  test:
    desc: Run all Go tests
    dir: backend
    cmds:
      - go test ./...

  lint:
    desc: Run golangci-lint on the backend
    dir: backend
    cmds:
      - golangci-lint run ./...
```

- [ ] **Step 6: Commit**

```bash
git init
git add Taskfile.yml docker-compose.yml .env.example .gitignore
git commit -m "chore: root infrastructure (Taskfile, docker-compose, env)"
```

---

## Task 2: Go Module + Domain Models + Stats

**Files:**
- Create: `backend/go.mod`
- Create: `backend/internal/domain/models.go`
- Create: `backend/internal/domain/stats.go`
- Create: `backend/internal/domain/stats_test.go`

- [ ] **Step 1: Initialise Go module**

```bash
cd backend
go mod init github.com/jplanckeel/thevaultbo
go mod tidy
```

- [ ] **Step 2: Write the failing tests for stats**

Create `backend/internal/domain/stats_test.go`:

```go
package domain_test

import (
	"testing"

	"github.com/jplanckeel/thevaultbo/internal/domain"
)

func TestComputeStats_Normal(t *testing.T) {
	prices := []float64{10.0, 20.0, 30.0, 40.0, 50.0}
	stats := domain.ComputeStats(prices)

	if stats.Min != 10.0 {
		t.Errorf("Min: want 10.0, got %f", stats.Min)
	}
	if stats.Max != 50.0 {
		t.Errorf("Max: want 50.0, got %f", stats.Max)
	}
	if stats.Median != 30.0 {
		t.Errorf("Median: want 30.0, got %f", stats.Median)
	}
	if stats.Count != 5 {
		t.Errorf("Count: want 5, got %d", stats.Count)
	}
}

func TestComputeStats_Even(t *testing.T) {
	prices := []float64{10.0, 20.0, 30.0, 40.0}
	stats := domain.ComputeStats(prices)
	if stats.Median != 25.0 {
		t.Errorf("Median (even): want 25.0, got %f", stats.Median)
	}
}

func TestComputeStats_Empty(t *testing.T) {
	stats := domain.ComputeStats(nil)
	if stats.Count != 0 {
		t.Errorf("Count: want 0, got %d", stats.Count)
	}
}

func TestComputeStats_Single(t *testing.T) {
	stats := domain.ComputeStats([]float64{42.0})
	if stats.Min != 42.0 || stats.Max != 42.0 || stats.Median != 42.0 {
		t.Errorf("Single: want all 42.0, got %+v", stats)
	}
}
```

- [ ] **Step 3: Run test — expect failure**

```bash
cd backend && go test ./internal/domain/...
```

Expected: `cannot find package "github.com/jplanckeel/thevaultbo/internal/domain"`

- [ ] **Step 4: Create `backend/internal/domain/models.go`**

```go
package domain

import "time"

// ItemType represents the category of a collectible.
type ItemType string

const (
	ItemTypeGame      ItemType = "game"
	ItemTypeConsole   ItemType = "console"
	ItemTypeAccessory ItemType = "accessory"
)

// PriceCondition represents the physical state used for price lookup.
type PriceCondition string

const (
	ConditionLoose PriceCondition = "loose"
	ConditionCIB   PriceCondition = "cib"
	ConditionNew   PriceCondition = "new"
)

// Item is a game, console, or accessory synced from the mobile app.
type Item struct {
	ID         string    `json:"id"`
	ExternalID string    `json:"external_id"`
	Type       ItemType  `json:"type"`
	Title      string    `json:"title"`
	Platform   string    `json:"platform"`
	Region     string    `json:"region"`
	Condition  string    `json:"condition"`
	Status     string    `json:"status"`
	CoverURL   string    `json:"cover_url"`
	CreatedAt  time.Time `json:"created_at"`
	UpdatedAt  time.Time `json:"updated_at"`
}

// PriceSnapshot is one scrape result for a given item + condition.
type PriceSnapshot struct {
	ID          string         `json:"id"`
	ItemID      string         `json:"item_id"`
	Source      string         `json:"source"`
	Condition   PriceCondition `json:"condition"`
	PriceMin    float64        `json:"price_min"`
	PriceMax    float64        `json:"price_max"`
	PriceMedian float64        `json:"price_median"`
	SampleCount int            `json:"sample_count"`
	Currency    string         `json:"currency"`
	ScrapedAt   time.Time      `json:"scraped_at"`
}

// ScrapeJob tracks a scraping run (cron or manual).
type ScrapeJob struct {
	ID          string     `json:"id"`
	TriggeredBy string     `json:"triggered_by"`
	Status      string     `json:"status"`
	ItemsTotal  int        `json:"items_total"`
	ItemsDone   int        `json:"items_done"`
	Error       string     `json:"error,omitempty"`
	StartedAt   time.Time  `json:"started_at"`
	FinishedAt  *time.Time `json:"finished_at,omitempty"`
}

// PriceStats holds computed statistics for a set of prices.
type PriceStats struct {
	Min    float64
	Max    float64
	Median float64
	Count  int
}

// LatestPrice holds the most recent prices for an item across all conditions.
type LatestPrice struct {
	ItemID string          `json:"item_id"`
	Prices map[string]*PriceSnapshot `json:"prices"` // key: "loose"|"cib"|"new"
}
```

- [ ] **Step 5: Create `backend/internal/domain/stats.go`**

```go
package domain

import "sort"

// ComputeStats computes min, max, median and count from a slice of prices.
// Returns zero-value PriceStats if prices is empty.
func ComputeStats(prices []float64) PriceStats {
	if len(prices) == 0 {
		return PriceStats{}
	}

	sorted := make([]float64, len(prices))
	copy(sorted, prices)
	sort.Float64s(sorted)

	n := len(sorted)
	var median float64
	if n%2 == 0 {
		median = (sorted[n/2-1] + sorted[n/2]) / 2
	} else {
		median = sorted[n/2]
	}

	return PriceStats{
		Min:    sorted[0],
		Max:    sorted[n-1],
		Median: median,
		Count:  n,
	}
}
```

- [ ] **Step 6: Run tests — expect pass**

```bash
cd backend && go test ./internal/domain/...
```

Expected: `ok  github.com/jplanckeel/thevaultbo/internal/domain`

- [ ] **Step 7: Commit**

```bash
git add backend/
git commit -m "feat(domain): add domain models and price stats computation"
```

---

## Task 3: Database Migrations

**Files:**
- Create: `backend/migrations/000001_create_items.up.sql`
- Create: `backend/migrations/000001_create_items.down.sql`
- Create: `backend/migrations/000002_create_price_snapshots.up.sql`
- Create: `backend/migrations/000002_create_price_snapshots.down.sql`
- Create: `backend/migrations/000003_create_scrape_jobs.up.sql`
- Create: `backend/migrations/000003_create_scrape_jobs.down.sql`
- Create: `backend/migrations/000004_create_settings.up.sql`
- Create: `backend/migrations/000004_create_settings.down.sql`

- [ ] **Step 1: Create `backend/migrations/000001_create_items.up.sql`**

```sql
CREATE TABLE IF NOT EXISTS items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id TEXT NOT NULL UNIQUE,
    type        TEXT NOT NULL,
    title       TEXT NOT NULL,
    platform    TEXT NOT NULL DEFAULT '',
    region      TEXT NOT NULL DEFAULT '',
    condition   TEXT NOT NULL DEFAULT '',
    status      TEXT NOT NULL DEFAULT 'owned',
    cover_url   TEXT NOT NULL DEFAULT '',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 2: Create `backend/migrations/000001_create_items.down.sql`**

```sql
DROP TABLE IF EXISTS items;
```

- [ ] **Step 3: Create `backend/migrations/000002_create_price_snapshots.up.sql`**

```sql
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
```

- [ ] **Step 4: Create `backend/migrations/000002_create_price_snapshots.down.sql`**

```sql
DROP TABLE IF EXISTS price_snapshots;
```

- [ ] **Step 5: Create `backend/migrations/000003_create_scrape_jobs.up.sql`**

```sql
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
```

- [ ] **Step 6: Create `backend/migrations/000003_create_scrape_jobs.down.sql`**

```sql
DROP TABLE IF EXISTS scrape_jobs;
```

- [ ] **Step 7: Create `backend/migrations/000004_create_settings.up.sql`**

```sql
CREATE TABLE IF NOT EXISTS settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL DEFAULT ''
);

INSERT INTO settings (key, value) VALUES
    ('ebay_marketplace',   'EBAY_FR'),
    ('scrape_schedule',    '0 2 * * *'),
    ('scrape_sample_size', '20')
ON CONFLICT (key) DO NOTHING;
```

- [ ] **Step 8: Create `backend/migrations/000004_create_settings.down.sql`**

```sql
DROP TABLE IF EXISTS settings;
```

- [ ] **Step 9: Commit**

```bash
git add backend/migrations/
git commit -m "feat(db): add database migrations for items, price_snapshots, scrape_jobs, settings"
```

---

## Task 4: Store Layer (Interfaces + DB + Items)

**Files:**
- Create: `backend/internal/store/store.go`
- Create: `backend/internal/store/db.go`
- Create: `backend/internal/store/items.go`

- [ ] **Step 1: Add dependencies**

```bash
cd backend
go get github.com/jackc/pgx/v5@latest
go get github.com/jackc/pgx/v5/pgxpool@latest
go get github.com/golang-migrate/migrate/v4@latest
go get github.com/golang-migrate/migrate/v4/database/postgres@latest
go get github.com/golang-migrate/migrate/v4/source/iofs@latest
go get github.com/google/uuid@latest
go mod tidy
```

- [ ] **Step 2: Create `backend/internal/store/store.go`**

```go
package store

import (
	"context"

	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// ItemStore defines persistence operations for items.
type ItemStore interface {
	Upsert(ctx context.Context, item domain.Item) (domain.Item, error)
	List(ctx context.Context) ([]domain.Item, error)
	GetByID(ctx context.Context, id string) (domain.Item, error)
}

// PriceStore defines persistence operations for price snapshots.
type PriceStore interface {
	Insert(ctx context.Context, snapshot domain.PriceSnapshot) (domain.PriceSnapshot, error)
	ListByItemID(ctx context.Context, itemID string) ([]domain.PriceSnapshot, error)
	LatestPerItem(ctx context.Context) ([]domain.PriceSnapshot, error)
}

// JobStore defines persistence operations for scrape jobs.
type JobStore interface {
	Create(ctx context.Context, job domain.ScrapeJob) (domain.ScrapeJob, error)
	Update(ctx context.Context, job domain.ScrapeJob) error
	GetByID(ctx context.Context, id string) (domain.ScrapeJob, error)
	List(ctx context.Context) ([]domain.ScrapeJob, error)
}

// SettingsStore defines read/write access to key-value settings.
type SettingsStore interface {
	Get(ctx context.Context, key string) (string, error)
	Set(ctx context.Context, key, value string) error
	GetAll(ctx context.Context) (map[string]string, error)
}
```

- [ ] **Step 3: Create `backend/internal/store/db.go`**

```go
package store

import (
	"context"
	"embed"
	"fmt"

	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	"github.com/golang-migrate/migrate/v4/source/iofs"
	"github.com/jackc/pgx/v5/pgxpool"
)

//go:embed ../../migrations/*.sql
var migrationsFS embed.FS

// OpenDB creates and validates a connection pool to PostgreSQL.
func OpenDB(ctx context.Context, databaseURL string) (*pgxpool.Pool, error) {
	pool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		return nil, fmt.Errorf("pgxpool.New: %w", err)
	}
	if err := pool.Ping(ctx); err != nil {
		return nil, fmt.Errorf("db ping: %w", err)
	}
	return pool, nil
}

// RunMigrations applies pending up-migrations from the embedded FS.
func RunMigrations(databaseURL string) error {
	src, err := iofs.New(migrationsFS, "migrations")
	if err != nil {
		return fmt.Errorf("iofs source: %w", err)
	}
	m, err := migrate.NewWithSourceInstance("iofs", src, databaseURL)
	if err != nil {
		return fmt.Errorf("migrate.New: %w", err)
	}
	if err := m.Up(); err != nil && err != migrate.ErrNoChange {
		return fmt.Errorf("migrate up: %w", err)
	}
	return nil
}
```

**Note:** The `//go:embed` path is relative to this file's location (`backend/internal/store/db.go`). The migrations are at `backend/migrations/`, so the embed path is `../../migrations/*.sql`. Adjust if the file layout differs.

- [ ] **Step 4: Create `backend/internal/store/items.go`**

```go
package store

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// PostgresItemStore implements ItemStore using pgx.
type PostgresItemStore struct {
	pool *pgxpool.Pool
}

// NewItemStore creates a new PostgresItemStore.
func NewItemStore(pool *pgxpool.Pool) *PostgresItemStore {
	return &PostgresItemStore{pool: pool}
}

// Upsert inserts or updates an item identified by external_id.
func (s *PostgresItemStore) Upsert(ctx context.Context, item domain.Item) (domain.Item, error) {
	const q = `
		INSERT INTO items (external_id, type, title, platform, region, condition, status, cover_url, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW(), NOW())
		ON CONFLICT (external_id) DO UPDATE SET
			type       = EXCLUDED.type,
			title      = EXCLUDED.title,
			platform   = EXCLUDED.platform,
			region     = EXCLUDED.region,
			condition  = EXCLUDED.condition,
			status     = EXCLUDED.status,
			cover_url  = EXCLUDED.cover_url,
			updated_at = NOW()
		RETURNING id, external_id, type, title, platform, region, condition, status, cover_url, created_at, updated_at
	`
	var out domain.Item
	err := s.pool.QueryRow(ctx, q,
		item.ExternalID, string(item.Type), item.Title, item.Platform,
		item.Region, item.Condition, item.Status, item.CoverURL,
	).Scan(
		&out.ID, &out.ExternalID, &out.Type, &out.Title,
		&out.Platform, &out.Region, &out.Condition, &out.Status,
		&out.CoverURL, &out.CreatedAt, &out.UpdatedAt,
	)
	if err != nil {
		return domain.Item{}, fmt.Errorf("item upsert: %w", err)
	}
	return out, nil
}

// List returns all items ordered by title.
func (s *PostgresItemStore) List(ctx context.Context) ([]domain.Item, error) {
	const q = `
		SELECT id, external_id, type, title, platform, region, condition, status, cover_url, created_at, updated_at
		FROM items ORDER BY title ASC
	`
	rows, err := s.pool.Query(ctx, q)
	if err != nil {
		return nil, fmt.Errorf("items list: %w", err)
	}
	defer rows.Close()

	var items []domain.Item
	for rows.Next() {
		var i domain.Item
		if err := rows.Scan(
			&i.ID, &i.ExternalID, &i.Type, &i.Title,
			&i.Platform, &i.Region, &i.Condition, &i.Status,
			&i.CoverURL, &i.CreatedAt, &i.UpdatedAt,
		); err != nil {
			return nil, fmt.Errorf("item scan: %w", err)
		}
		items = append(items, i)
	}
	return items, rows.Err()
}

// GetByID returns an item by its UUID.
func (s *PostgresItemStore) GetByID(ctx context.Context, id string) (domain.Item, error) {
	const q = `
		SELECT id, external_id, type, title, platform, region, condition, status, cover_url, created_at, updated_at
		FROM items WHERE id = $1
	`
	var i domain.Item
	err := s.pool.QueryRow(ctx, q, id).Scan(
		&i.ID, &i.ExternalID, &i.Type, &i.Title,
		&i.Platform, &i.Region, &i.Condition, &i.Status,
		&i.CoverURL, &i.CreatedAt, &i.UpdatedAt,
	)
	if err != nil {
		return domain.Item{}, fmt.Errorf("item get: %w", err)
	}
	return i, nil
}
```

- [ ] **Step 5: Verify compilation**

```bash
cd backend && go build ./...
```

Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add backend/
git commit -m "feat(store): add store interfaces, DB connection, and items store"
```

---

## Task 5: Store Layer (Prices + Jobs + Settings)

**Files:**
- Create: `backend/internal/store/prices.go`
- Create: `backend/internal/store/jobs.go`
- Create: `backend/internal/store/settings.go`

- [ ] **Step 1: Create `backend/internal/store/prices.go`**

```go
package store

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// PostgresPriceStore implements PriceStore using pgx.
type PostgresPriceStore struct {
	pool *pgxpool.Pool
}

// NewPriceStore creates a new PostgresPriceStore.
func NewPriceStore(pool *pgxpool.Pool) *PostgresPriceStore {
	return &PostgresPriceStore{pool: pool}
}

// Insert stores a new price snapshot.
func (s *PostgresPriceStore) Insert(ctx context.Context, snap domain.PriceSnapshot) (domain.PriceSnapshot, error) {
	const q = `
		INSERT INTO price_snapshots (item_id, source, condition, price_min, price_max, price_median, sample_count, currency, scraped_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW())
		RETURNING id, item_id, source, condition, price_min, price_max, price_median, sample_count, currency, scraped_at
	`
	var out domain.PriceSnapshot
	err := s.pool.QueryRow(ctx, q,
		snap.ItemID, snap.Source, string(snap.Condition),
		snap.PriceMin, snap.PriceMax, snap.PriceMedian,
		snap.SampleCount, snap.Currency,
	).Scan(
		&out.ID, &out.ItemID, &out.Source, &out.Condition,
		&out.PriceMin, &out.PriceMax, &out.PriceMedian,
		&out.SampleCount, &out.Currency, &out.ScrapedAt,
	)
	if err != nil {
		return domain.PriceSnapshot{}, fmt.Errorf("price insert: %w", err)
	}
	return out, nil
}

// ListByItemID returns all snapshots for one item, newest first.
func (s *PostgresPriceStore) ListByItemID(ctx context.Context, itemID string) ([]domain.PriceSnapshot, error) {
	const q = `
		SELECT id, item_id, source, condition, price_min, price_max, price_median, sample_count, currency, scraped_at
		FROM price_snapshots WHERE item_id = $1 ORDER BY scraped_at DESC
	`
	rows, err := s.pool.Query(ctx, q, itemID)
	if err != nil {
		return nil, fmt.Errorf("prices list: %w", err)
	}
	defer rows.Close()

	var snaps []domain.PriceSnapshot
	for rows.Next() {
		var p domain.PriceSnapshot
		if err := rows.Scan(
			&p.ID, &p.ItemID, &p.Source, &p.Condition,
			&p.PriceMin, &p.PriceMax, &p.PriceMedian,
			&p.SampleCount, &p.Currency, &p.ScrapedAt,
		); err != nil {
			return nil, fmt.Errorf("price scan: %w", err)
		}
		snaps = append(snaps, p)
	}
	return snaps, rows.Err()
}

// LatestPerItem returns the single most recent snapshot per (item_id, condition).
func (s *PostgresPriceStore) LatestPerItem(ctx context.Context) ([]domain.PriceSnapshot, error) {
	const q = `
		SELECT DISTINCT ON (item_id, condition)
			id, item_id, source, condition, price_min, price_max, price_median, sample_count, currency, scraped_at
		FROM price_snapshots
		ORDER BY item_id, condition, scraped_at DESC
	`
	rows, err := s.pool.Query(ctx, q)
	if err != nil {
		return nil, fmt.Errorf("latest prices: %w", err)
	}
	defer rows.Close()

	var snaps []domain.PriceSnapshot
	for rows.Next() {
		var p domain.PriceSnapshot
		if err := rows.Scan(
			&p.ID, &p.ItemID, &p.Source, &p.Condition,
			&p.PriceMin, &p.PriceMax, &p.PriceMedian,
			&p.SampleCount, &p.Currency, &p.ScrapedAt,
		); err != nil {
			return nil, fmt.Errorf("latest price scan: %w", err)
		}
		snaps = append(snaps, p)
	}
	return snaps, rows.Err()
}
```

- [ ] **Step 2: Create `backend/internal/store/jobs.go`**

```go
package store

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// PostgresJobStore implements JobStore using pgx.
type PostgresJobStore struct {
	pool *pgxpool.Pool
}

// NewJobStore creates a new PostgresJobStore.
func NewJobStore(pool *pgxpool.Pool) *PostgresJobStore {
	return &PostgresJobStore{pool: pool}
}

// Create inserts a new scrape job and returns it with its generated ID.
func (s *PostgresJobStore) Create(ctx context.Context, job domain.ScrapeJob) (domain.ScrapeJob, error) {
	const q = `
		INSERT INTO scrape_jobs (triggered_by, status, items_total, items_done, error, started_at)
		VALUES ($1, $2, $3, $4, $5, NOW())
		RETURNING id, triggered_by, status, items_total, items_done, error, started_at, finished_at
	`
	return s.scan(s.pool.QueryRow(ctx, q,
		job.TriggeredBy, job.Status, job.ItemsTotal, job.ItemsDone, job.Error,
	))
}

// Update persists status, items_done, error, and finished_at for a job.
func (s *PostgresJobStore) Update(ctx context.Context, job domain.ScrapeJob) error {
	const q = `
		UPDATE scrape_jobs SET status=$1, items_done=$2, error=$3, finished_at=$4
		WHERE id=$5
	`
	_, err := s.pool.Exec(ctx, q, job.Status, job.ItemsDone, job.Error, job.FinishedAt, job.ID)
	return err
}

// GetByID returns a job by its UUID.
func (s *PostgresJobStore) GetByID(ctx context.Context, id string) (domain.ScrapeJob, error) {
	const q = `
		SELECT id, triggered_by, status, items_total, items_done, error, started_at, finished_at
		FROM scrape_jobs WHERE id = $1
	`
	return s.scan(s.pool.QueryRow(ctx, q, id))
}

// List returns all jobs ordered by start time descending.
func (s *PostgresJobStore) List(ctx context.Context) ([]domain.ScrapeJob, error) {
	const q = `
		SELECT id, triggered_by, status, items_total, items_done, error, started_at, finished_at
		FROM scrape_jobs ORDER BY started_at DESC
	`
	rows, err := s.pool.Query(ctx, q)
	if err != nil {
		return nil, fmt.Errorf("jobs list: %w", err)
	}
	defer rows.Close()

	var jobs []domain.ScrapeJob
	for rows.Next() {
		var j domain.ScrapeJob
		if err := rows.Scan(
			&j.ID, &j.TriggeredBy, &j.Status, &j.ItemsTotal,
			&j.ItemsDone, &j.Error, &j.StartedAt, &j.FinishedAt,
		); err != nil {
			return nil, fmt.Errorf("job scan: %w", err)
		}
		jobs = append(jobs, j)
	}
	return jobs, rows.Err()
}

type scanner interface {
	Scan(dest ...any) error
}

func (s *PostgresJobStore) scan(row scanner) (domain.ScrapeJob, error) {
	var j domain.ScrapeJob
	err := row.Scan(
		&j.ID, &j.TriggeredBy, &j.Status, &j.ItemsTotal,
		&j.ItemsDone, &j.Error, &j.StartedAt, &j.FinishedAt,
	)
	if err != nil {
		return domain.ScrapeJob{}, fmt.Errorf("job scan: %w", err)
	}
	return j, nil
}
```

- [ ] **Step 3: Create `backend/internal/store/settings.go`**

```go
package store

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
)

// PostgresSettingsStore implements SettingsStore using pgx.
type PostgresSettingsStore struct {
	pool *pgxpool.Pool
}

// NewSettingsStore creates a new PostgresSettingsStore.
func NewSettingsStore(pool *pgxpool.Pool) *PostgresSettingsStore {
	return &PostgresSettingsStore{pool: pool}
}

// Get returns the value for a given key.
func (s *PostgresSettingsStore) Get(ctx context.Context, key string) (string, error) {
	var value string
	err := s.pool.QueryRow(ctx, `SELECT value FROM settings WHERE key=$1`, key).Scan(&value)
	if err != nil {
		return "", fmt.Errorf("settings get %q: %w", key, err)
	}
	return value, nil
}

// Set inserts or updates a setting.
func (s *PostgresSettingsStore) Set(ctx context.Context, key, value string) error {
	_, err := s.pool.Exec(ctx,
		`INSERT INTO settings (key, value) VALUES ($1, $2) ON CONFLICT (key) DO UPDATE SET value=EXCLUDED.value`,
		key, value,
	)
	return err
}

// GetAll returns all settings as a map.
func (s *PostgresSettingsStore) GetAll(ctx context.Context) (map[string]string, error) {
	rows, err := s.pool.Query(ctx, `SELECT key, value FROM settings`)
	if err != nil {
		return nil, fmt.Errorf("settings getAll: %w", err)
	}
	defer rows.Close()

	m := make(map[string]string)
	for rows.Next() {
		var k, v string
		if err := rows.Scan(&k, &v); err != nil {
			return nil, err
		}
		m[k] = v
	}
	return m, rows.Err()
}
```

- [ ] **Step 4: Verify compilation**

```bash
cd backend && go build ./...
```

Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add backend/internal/store/
git commit -m "feat(store): add prices, jobs, and settings store implementations"
```

---

## Task 6: Items API Handlers

**Files:**
- Create: `backend/internal/api/handlers/items.go`
- Create: `backend/internal/api/handlers/items_test.go`
- Create: `backend/internal/api/router.go`

- [ ] **Step 1: Add chi and cors dependency**

```bash
cd backend
go get github.com/go-chi/chi/v5@latest
go get github.com/go-chi/cors@latest
go mod tidy
```

- [ ] **Step 2: Write the failing handler tests**

Create `backend/internal/api/handlers/items_test.go`:

```go
package handlers_test

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// mockItemStore is an in-memory ItemStore for testing.
type mockItemStore struct {
	items map[string]domain.Item
}

func newMockItemStore() *mockItemStore {
	return &mockItemStore{items: make(map[string]domain.Item)}
}

func (m *mockItemStore) Upsert(_ context.Context, item domain.Item) (domain.Item, error) {
	if item.ID == "" {
		item.ID = item.ExternalID
	}
	m.items[item.ExternalID] = item
	return item, nil
}

func (m *mockItemStore) List(_ context.Context) ([]domain.Item, error) {
	result := make([]domain.Item, 0, len(m.items))
	for _, v := range m.items {
		result = append(result, v)
	}
	return result, nil
}

func (m *mockItemStore) GetByID(_ context.Context, id string) (domain.Item, error) {
	for _, v := range m.items {
		if v.ID == id {
			return v, nil
		}
	}
	return domain.Item{}, fmt.Errorf("not found")
}

func TestUpsertItem_Returns201(t *testing.T) {
	store := newMockItemStore()
	h := handlers.NewItemsHandler(store, nil)

	body, _ := json.Marshal(domain.Item{
		ExternalID: "ext-001",
		Type:       domain.ItemTypeGame,
		Title:      "Super Mario 64",
		Platform:   "Nintendo 64",
	})

	req := httptest.NewRequest(http.MethodPost, "/api/v1/items", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	h.UpsertItem(w, req)

	if w.Code != http.StatusCreated {
		t.Errorf("want 201, got %d", w.Code)
	}
	var resp domain.Item
	json.NewDecoder(w.Body).Decode(&resp)
	if resp.Title != "Super Mario 64" {
		t.Errorf("want title 'Super Mario 64', got %q", resp.Title)
	}
}

func TestListItems_ReturnsJSON(t *testing.T) {
	store := newMockItemStore()
	store.items["ext-001"] = domain.Item{ID: "1", ExternalID: "ext-001", Title: "Zelda"}

	h := handlers.NewItemsHandler(store, nil)
	req := httptest.NewRequest(http.MethodGet, "/api/v1/items", nil)
	w := httptest.NewRecorder()

	h.ListItems(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("want 200, got %d", w.Code)
	}
	var items []domain.Item
	json.NewDecoder(w.Body).Decode(&items)
	if len(items) != 1 {
		t.Errorf("want 1 item, got %d", len(items))
	}
}
```

- [ ] **Step 3: Run test — expect compile error**

```bash
cd backend && go test ./internal/api/handlers/...
```

Expected: compile error — `handlers.NewItemsHandler` not defined.

- [ ] **Step 4: Create `backend/internal/api/handlers/items.go`**

```go
package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// ItemsHandler handles HTTP requests for items.
type ItemsHandler struct {
	items  store.ItemStore
	prices store.PriceStore
}

// NewItemsHandler creates a new ItemsHandler.
func NewItemsHandler(items store.ItemStore, prices store.PriceStore) *ItemsHandler {
	return &ItemsHandler{items: items, prices: prices}
}

// UpsertItem handles POST /api/v1/items — upsert from mobile app.
func (h *ItemsHandler) UpsertItem(w http.ResponseWriter, r *http.Request) {
	var item domain.Item
	if err := json.NewDecoder(r.Body).Decode(&item); err != nil {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}
	if item.ExternalID == "" {
		http.Error(w, "external_id is required", http.StatusBadRequest)
		return
	}

	saved, err := h.items.Upsert(r.Context(), item)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(saved)
}

// ListItems handles GET /api/v1/items.
func (h *ItemsHandler) ListItems(w http.ResponseWriter, r *http.Request) {
	items, err := h.items.List(r.Context())
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if items == nil {
		items = []domain.Item{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(items)
}

// GetItem handles GET /api/v1/items/{id}.
func (h *ItemsHandler) GetItem(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	item, err := h.items.GetByID(r.Context(), id)
	if err != nil {
		http.Error(w, "not found", http.StatusNotFound)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(item)
}

// GetItemPrices handles GET /api/v1/items/{id}/prices.
func (h *ItemsHandler) GetItemPrices(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	snaps, err := h.prices.ListByItemID(r.Context(), id)
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if snaps == nil {
		snaps = []domain.PriceSnapshot{}
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(snaps)
}
```

- [ ] **Step 5: Fix the test file — add missing import**

Add `"fmt"` to the imports in `items_test.go`:

```go
import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)
```

- [ ] **Step 6: Run tests — expect pass**

```bash
cd backend && go test ./internal/api/handlers/...
```

Expected: `ok  github.com/jplanckeel/thevaultbo/internal/api/handlers`

- [ ] **Step 7: Create `backend/internal/api/router.go`**

```go
package api

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
	"github.com/jplanckeel/thevaultbo/internal/api/handlers"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// NewRouter builds and returns the chi router with all routes mounted.
func NewRouter(
	items store.ItemStore,
	prices store.PriceStore,
	jobs store.JobStore,
	settings store.SettingsStore,
	ebayRunner func(triggeredBy string, itemIDs ...string),
) http.Handler {
	r := chi.NewRouter()

	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins: []string{"*"},
		AllowedMethods: []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders: []string{"Accept", "Content-Type"},
	}))

	ih := handlers.NewItemsHandler(items, prices)
	ph := handlers.NewPricesHandler(prices)
	sh := handlers.NewScrapeHandler(jobs, ebayRunner, items)

	r.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("ok"))
	})

	r.Route("/api/v1", func(r chi.Router) {
		r.Post("/items", ih.UpsertItem)
		r.Get("/items", ih.ListItems)
		r.Get("/items/{id}", ih.GetItem)
		r.Get("/items/{id}/prices", ih.GetItemPrices)

		r.Get("/prices/latest", ph.GetLatestPrices)

		r.Post("/scrape", sh.TriggerFullScrape)
		r.Post("/scrape/items/{id}", sh.TriggerItemScrape)
		r.Get("/scrape/jobs", sh.ListJobs)
		r.Get("/scrape/jobs/{id}", sh.GetJob)
	})

	return r
}
```

- [ ] **Step 8: Commit**

```bash
git add backend/
git commit -m "feat(api): add items handlers and chi router"
```

---

## Task 7: Prices & Scrape Handlers

**Files:**
- Create: `backend/internal/api/handlers/prices.go`
- Create: `backend/internal/api/handlers/scrape.go`

- [ ] **Step 1: Create `backend/internal/api/handlers/prices.go`**

```go
package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/jplanckeel/thevaultbo/internal/store"
)

// PricesHandler handles HTTP requests for price data.
type PricesHandler struct {
	prices store.PriceStore
}

// NewPricesHandler creates a new PricesHandler.
func NewPricesHandler(prices store.PriceStore) *PricesHandler {
	return &PricesHandler{prices: prices}
}

// GetLatestPrices handles GET /api/v1/prices/latest.
// Returns the most recent price snapshot per (item_id, condition).
func (h *PricesHandler) GetLatestPrices(w http.ResponseWriter, r *http.Request) {
	snaps, err := h.prices.LatestPerItem(r.Context())
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if snaps == nil {
		snaps = []domain.PriceSnapshot{}
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(snaps)
}
```

**Note:** Add the missing import at top of the file:

```go
import (
	"encoding/json"
	"net/http"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)
```

- [ ] **Step 2: Create `backend/internal/api/handlers/scrape.go`**

```go
package handlers

import (
	"context"
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// ScrapeHandler handles HTTP requests for scrape job management.
type ScrapeHandler struct {
	jobs        store.JobStore
	items       store.ItemStore
	ebayRunner  func(triggeredBy string, itemIDs ...string)
}

// NewScrapeHandler creates a new ScrapeHandler.
func NewScrapeHandler(jobs store.JobStore, runner func(string, ...string), items store.ItemStore) *ScrapeHandler {
	return &ScrapeHandler{jobs: jobs, ebayRunner: runner, items: items}
}

// TriggerFullScrape handles POST /api/v1/scrape — triggers a full scrape in background.
func (h *ScrapeHandler) TriggerFullScrape(w http.ResponseWriter, r *http.Request) {
	go h.ebayRunner("manual")
	w.WriteHeader(http.StatusAccepted)
	w.Write([]byte(`{"message":"scrape job started"}`))
}

// TriggerItemScrape handles POST /api/v1/scrape/items/{id} — scrapes a single item.
func (h *ScrapeHandler) TriggerItemScrape(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	if _, err := h.items.GetByID(r.Context(), id); err != nil {
		http.Error(w, "item not found", http.StatusNotFound)
		return
	}
	go h.ebayRunner("manual", id)
	w.WriteHeader(http.StatusAccepted)
	w.Write([]byte(`{"message":"scrape job started"}`))
}

// ListJobs handles GET /api/v1/scrape/jobs.
func (h *ScrapeHandler) ListJobs(w http.ResponseWriter, r *http.Request) {
	jobs, err := h.jobs.List(r.Context())
	if err != nil {
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	if jobs == nil {
		jobs = []domain.ScrapeJob{}
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(jobs)
}

// GetJob handles GET /api/v1/scrape/jobs/{id}.
func (h *ScrapeHandler) GetJob(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	job, err := h.jobs.GetByID(r.Context(), id)
	if err != nil {
		http.Error(w, "not found", http.StatusNotFound)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(job)
}
```

**Note:** Add missing import for `domain` in `scrape.go`:

```go
import (
	"context"
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)
```

- [ ] **Step 3: Verify compilation**

```bash
cd backend && go build ./...
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add backend/internal/api/handlers/
git commit -m "feat(api): add prices and scrape handlers"
```

---

## Task 8: eBay Scraper

**Files:**
- Create: `backend/internal/scraper/ebay.go`
- Create: `backend/internal/scraper/ebay_test.go`

- [ ] **Step 1: Write the failing tests**

Create `backend/internal/scraper/ebay_test.go`:

```go
package scraper_test

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/jplanckeel/thevaultbo/internal/scraper"
)

func TestFetchPrices_ParsesResponse(t *testing.T) {
	// Fake eBay API server
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/identity/v1/oauth2/token" {
			json.NewEncoder(w).Encode(map[string]any{
				"access_token": "fake-token",
				"expires_in":   7200,
			})
			return
		}
		json.NewEncoder(w).Encode(map[string]any{
			"itemSummaries": []map[string]any{
				{"price": map[string]any{"value": "15.00", "currency": "EUR"}},
				{"price": map[string]any{"value": "25.00", "currency": "EUR"}},
				{"price": map[string]any{"value": "20.00", "currency": "EUR"}},
			},
		})
	}))
	defer srv.Close()

	client := scraper.NewEbayClientWithBaseURL("fake-id", "fake-secret", "EBAY_FR", srv.URL)
	stats, err := client.FetchPrices("Super Mario 64", "Nintendo 64", "loose", 20)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if stats.Count != 3 {
		t.Errorf("Count: want 3, got %d", stats.Count)
	}
	if stats.Min != 15.0 {
		t.Errorf("Min: want 15.0, got %f", stats.Min)
	}
	if stats.Max != 25.0 {
		t.Errorf("Max: want 25.0, got %f", stats.Max)
	}
	if stats.Median != 20.0 {
		t.Errorf("Median: want 20.0, got %f", stats.Median)
	}
}
```

- [ ] **Step 2: Run test — expect failure**

```bash
cd backend && go test ./internal/scraper/...
```

Expected: compile error — `scraper.NewEbayClientWithBaseURL` not defined.

- [ ] **Step 3: Create `backend/internal/scraper/ebay.go`**

```go
package scraper

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/domain"
)

const (
	ebayBaseURL  = "https://api.ebay.com"
	tokenPath    = "/identity/v1/oauth2/token"
	searchPath   = "/buy/browse/v1/item_summary/search"
	ebayScope    = "https://api.ebay.com/oauth/api_scope"
)

// EbayClient calls the eBay Browse API to collect price data.
type EbayClient struct {
	clientID     string
	clientSecret string
	marketplace  string
	baseURL      string
	httpClient   *http.Client
	mu           sync.Mutex
	token        string
	tokenExpiry  time.Time
}

// NewEbayClient creates a production EbayClient using the real eBay API.
func NewEbayClient(clientID, clientSecret, marketplace string) *EbayClient {
	return NewEbayClientWithBaseURL(clientID, clientSecret, marketplace, ebayBaseURL)
}

// NewEbayClientWithBaseURL creates an EbayClient with a configurable base URL (for testing).
func NewEbayClientWithBaseURL(clientID, clientSecret, marketplace, baseURL string) *EbayClient {
	return &EbayClient{
		clientID:     clientID,
		clientSecret: clientSecret,
		marketplace:  marketplace,
		baseURL:      baseURL,
		httpClient:   &http.Client{Timeout: 15 * time.Second},
	}
}

// FetchPrices searches eBay for a given item+condition and returns price statistics.
// condition must be "loose", "cib", or "new".
func (c *EbayClient) FetchPrices(title, platform, condition string, limit int) (domain.PriceStats, error) {
	token, err := c.getToken()
	if err != nil {
		return domain.PriceStats{}, fmt.Errorf("ebay token: %w", err)
	}

	query := buildQuery(title, platform, condition)
	ebayCondition := conditionFilter(condition)

	searchURL := fmt.Sprintf("%s%s?q=%s&filter=conditions%%3A%%7B%s%%7D&limit=%d",
		c.baseURL, searchPath,
		url.QueryEscape(query), ebayCondition, limit,
	)

	req, err := http.NewRequest(http.MethodGet, searchURL, nil)
	if err != nil {
		return domain.PriceStats{}, err
	}
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("X-EBAY-C-MARKETPLACE-ID", c.marketplace)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return domain.PriceStats{}, fmt.Errorf("ebay search: %w", err)
	}
	defer resp.Body.Close()

	var result struct {
		ItemSummaries []struct {
			Price struct {
				Value    string `json:"value"`
				Currency string `json:"currency"`
			} `json:"price"`
		} `json:"itemSummaries"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return domain.PriceStats{}, fmt.Errorf("ebay decode: %w", err)
	}

	prices := make([]float64, 0, len(result.ItemSummaries))
	for _, item := range result.ItemSummaries {
		if v, err := strconv.ParseFloat(item.Price.Value, 64); err == nil {
			prices = append(prices, v)
		}
	}

	return domain.ComputeStats(prices), nil
}

// getToken returns a valid OAuth token, refreshing it if expired.
func (c *EbayClient) getToken() (string, error) {
	c.mu.Lock()
	defer c.mu.Unlock()

	if c.token != "" && time.Now().Before(c.tokenExpiry) {
		return c.token, nil
	}

	creds := base64.StdEncoding.EncodeToString([]byte(c.clientID + ":" + c.clientSecret))
	body := url.Values{}
	body.Set("grant_type", "client_credentials")
	body.Set("scope", ebayScope)

	req, err := http.NewRequest(http.MethodPost, c.baseURL+tokenPath, strings.NewReader(body.Encode()))
	if err != nil {
		return "", err
	}
	req.Header.Set("Authorization", "Basic "+creds)
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("ebay token request: %w", err)
	}
	defer resp.Body.Close()

	var result struct {
		AccessToken string `json:"access_token"`
		ExpiresIn   int    `json:"expires_in"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return "", fmt.Errorf("ebay token decode: %w", err)
	}

	c.token = result.AccessToken
	c.tokenExpiry = time.Now().Add(time.Duration(result.ExpiresIn-60) * time.Second)
	return c.token, nil
}

// buildQuery constructs the eBay search query string for a given condition.
func buildQuery(title, platform, condition string) string {
	base := title + " " + platform
	if condition == "cib" {
		return base + " complet"
	}
	return base
}

// conditionFilter returns the eBay condition filter value.
func conditionFilter(condition string) string {
	if condition == "new" {
		return "NEW"
	}
	return "USED"
}
```

- [ ] **Step 4: Run tests — expect pass**

```bash
cd backend && go test ./internal/scraper/...
```

Expected: `ok  github.com/jplanckeel/thevaultbo/internal/scraper`

- [ ] **Step 5: Commit**

```bash
git add backend/internal/scraper/
git commit -m "feat(scraper): add eBay Browse API client with OAuth token refresh"
```

---

## Task 9: Scheduler

**Files:**
- Create: `backend/internal/scheduler/scheduler.go`

- [ ] **Step 1: Add robfig/cron dependency**

```bash
cd backend
go get github.com/robfig/cron/v3@latest
go mod tidy
```

- [ ] **Step 2: Create `backend/internal/scheduler/scheduler.go`**

```go
package scheduler

import (
	"context"
	"log"
	"time"

	"github.com/robfig/cron/v3"
	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/scraper"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// Scheduler wraps robfig/cron and manages the price-scraping job.
type Scheduler struct {
	cron     *cron.Cron
	items    store.ItemStore
	prices   store.PriceStore
	jobs     store.JobStore
	settings store.SettingsStore
	ebay     *scraper.EbayClient
}

// New creates and starts a Scheduler. Call Stop() on shutdown.
func New(
	items store.ItemStore,
	prices store.PriceStore,
	jobs store.JobStore,
	settings store.SettingsStore,
	ebay *scraper.EbayClient,
) *Scheduler {
	return &Scheduler{
		cron:     cron.New(),
		items:    items,
		prices:   prices,
		jobs:     jobs,
		settings: settings,
		ebay:     ebay,
	}
}

// Start registers the cron job using the schedule stored in settings and begins ticking.
func (s *Scheduler) Start(ctx context.Context) error {
	schedule, err := s.settings.Get(ctx, "scrape_schedule")
	if err != nil || schedule == "" {
		schedule = "0 2 * * *"
	}

	if _, err := s.cron.AddFunc(schedule, func() {
		s.Run("cron")
	}); err != nil {
		return err
	}

	s.cron.Start()
	log.Printf("scheduler: started with schedule %q", schedule)
	return nil
}

// Stop gracefully shuts down the cron scheduler.
func (s *Scheduler) Stop() {
	s.cron.Stop()
}

// Run executes a full scrape for all items (or a subset if itemIDs are provided).
// triggeredBy should be "cron" or "manual".
func (s *Scheduler) Run(triggeredBy string, itemIDs ...string) {
	ctx := context.Background()

	allItems, err := s.items.List(ctx)
	if err != nil {
		log.Printf("scheduler: failed to list items: %v", err)
		return
	}

	var targets []domain.Item
	if len(itemIDs) == 0 {
		targets = allItems
	} else {
		idSet := make(map[string]bool, len(itemIDs))
		for _, id := range itemIDs {
			idSet[id] = true
		}
		for _, item := range allItems {
			if idSet[item.ID] {
				targets = append(targets, item)
			}
		}
	}

	job, err := s.jobs.Create(ctx, domain.ScrapeJob{
		TriggeredBy: triggeredBy,
		Status:      "running",
		ItemsTotal:  len(targets),
	})
	if err != nil {
		log.Printf("scheduler: failed to create job: %v", err)
		return
	}
	log.Printf("scheduler: job %s started (%d items)", job.ID, len(targets))

	conditions := []domain.PriceCondition{domain.ConditionLoose, domain.ConditionCIB, domain.ConditionNew}

	sampleSize := 20
	if ss, err := s.settings.Get(ctx, "scrape_sample_size"); err == nil {
		if n := parseInt(ss, 20); n > 0 {
			sampleSize = n
		}
	}

	for _, item := range targets {
		for _, cond := range conditions {
			stats, err := s.ebay.FetchPrices(item.Title, item.Platform, string(cond), sampleSize)
			if err != nil {
				log.Printf("scheduler: ebay error for %s/%s: %v", item.Title, cond, err)
				continue
			}
			if stats.Count == 0 {
				continue
			}
			snap := domain.PriceSnapshot{
				ItemID:      item.ID,
				Source:      "ebay",
				Condition:   cond,
				PriceMin:    stats.Min,
				PriceMax:    stats.Max,
				PriceMedian: stats.Median,
				SampleCount: stats.Count,
				Currency:    "EUR",
			}
			if _, err := s.prices.Insert(ctx, snap); err != nil {
				log.Printf("scheduler: insert snapshot error: %v", err)
			}
			// Respectful rate limit: 0.5s between eBay requests
			time.Sleep(500 * time.Millisecond)
		}

		job.ItemsDone++
		if err := s.jobs.Update(ctx, job); err != nil {
			log.Printf("scheduler: job update error: %v", err)
		}
	}

	now := time.Now()
	job.Status = "done"
	job.FinishedAt = &now
	if err := s.jobs.Update(ctx, job); err != nil {
		log.Printf("scheduler: job final update error: %v", err)
	}
	log.Printf("scheduler: job %s done", job.ID)
}

func parseInt(s string, def int) int {
	n := def
	if _, err := fmt.Sscanf(s, "%d", &n); err != nil {
		return def
	}
	return n
}
```

**Note:** Add `"fmt"` to the imports in `scheduler.go`.

- [ ] **Step 3: Verify compilation**

```bash
cd backend && go build ./...
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add backend/internal/scheduler/
git commit -m "feat(scheduler): add cron scheduler with eBay scrape runner"
```

---

## Task 10: Server Entrypoint + Air Config

**Files:**
- Create: `backend/cmd/server/main.go`
- Create: `backend/.air.toml`

- [ ] **Step 1: Add godotenv dependency**

```bash
cd backend
go get github.com/joho/godotenv@latest
go mod tidy
```

- [ ] **Step 2: Create `backend/.air.toml`**

```toml
root = "."
tmp_dir = ".air"

[build]
  cmd = "go build -o .air/server ./cmd/server"
  bin = ".air/server"
  full_bin = ".air/server"
  include_ext = ["go"]
  exclude_dir = ["migrations", ".air", "bin"]
  delay = 500

[log]
  time = true
```

- [ ] **Step 3: Create `backend/cmd/server/main.go`**

```go
package main

import (
	"context"
	"log"
	"net/http"
	"os"

	"github.com/joho/godotenv"
	"github.com/jplanckeel/thevaultbo/internal/api"
	"github.com/jplanckeel/thevaultbo/internal/scheduler"
	"github.com/jplanckeel/thevaultbo/internal/scraper"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

func main() {
	// Load .env if present (dev only)
	_ = godotenv.Load("../.env")

	databaseURL := mustEnv("DATABASE_URL")
	port := getEnv("PORT", "8080")
	ebayClientID := mustEnv("EBAY_CLIENT_ID")
	ebayClientSecret := mustEnv("EBAY_CLIENT_SECRET")

	ctx := context.Background()

	// Run DB migrations
	if err := store.RunMigrations(databaseURL); err != nil {
		log.Fatalf("migrations: %v", err)
	}
	log.Println("migrations: up to date")

	// Open connection pool
	pool, err := store.OpenDB(ctx, databaseURL)
	if err != nil {
		log.Fatalf("db: %v", err)
	}
	defer pool.Close()

	// Build stores
	itemStore := store.NewItemStore(pool)
	priceStore := store.NewPriceStore(pool)
	jobStore := store.NewJobStore(pool)
	settingsStore := store.NewSettingsStore(pool)

	// Build eBay client
	marketplace, _ := settingsStore.Get(ctx, "ebay_marketplace")
	if marketplace == "" {
		marketplace = "EBAY_FR"
	}
	ebayClient := scraper.NewEbayClient(ebayClientID, ebayClientSecret, marketplace)

	// Build and start scheduler
	sched := scheduler.New(itemStore, priceStore, jobStore, settingsStore, ebayClient)
	if err := sched.Start(ctx); err != nil {
		log.Fatalf("scheduler: %v", err)
	}
	defer sched.Stop()

	// ebayRunner is the function passed to HTTP handlers to trigger scrapes
	ebayRunner := func(triggeredBy string, itemIDs ...string) {
		sched.Run(triggeredBy, itemIDs...)
	}

	// Build HTTP server
	router := api.NewRouter(itemStore, priceStore, jobStore, settingsStore, ebayRunner)
	srv := &http.Server{
		Addr:    ":" + port,
		Handler: router,
	}

	log.Printf("server: listening on :%s", port)
	if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("server: %v", err)
	}
}

func mustEnv(key string) string {
	v := os.Getenv(key)
	if v == "" {
		log.Fatalf("required env var %q is not set", key)
	}
	return v
}

func getEnv(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}
```

- [ ] **Step 4: Verify the full backend builds**

```bash
cd backend && go build ./...
```

Expected: no errors.

- [ ] **Step 5: Run all backend tests**

```bash
cd backend && go test ./...
```

Expected: all tests pass.

- [ ] **Step 6: Start DB and test the backend end-to-end**

```bash
# From repo root
task dev:db
cd backend && air
# In another terminal:
curl http://localhost:8080/healthz
```

Expected: `ok`

```bash
curl -s -X POST http://localhost:8080/api/v1/items \
  -H "Content-Type: application/json" \
  -d '{"external_id":"test-1","type":"game","title":"Super Mario 64","platform":"Nintendo 64"}'
```

Expected: `201` with item JSON including a UUID `id`.

- [ ] **Step 7: Commit**

```bash
git add backend/
git commit -m "feat(server): wire all dependencies and add server entrypoint"
```

---

## Task 11: Frontend Scaffold

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/index.html`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/App.tsx`

- [ ] **Step 1: Scaffold the Vite project**

```bash
cd frontend
npm create vite@latest . -- --template react-ts
npm install
```

- [ ] **Step 2: Install dependencies**

```bash
npm install @semcore/ui@latest @semcore/d3-chart@latest react-router-dom@latest
npm install -D @types/react @types/react-dom
```

- [ ] **Step 3: Update `frontend/vite.config.ts`**

```ts
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_API_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
```

- [ ] **Step 4: Create `frontend/src/main.tsx`**

```tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import '@semcore/ui/style/index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
```

- [ ] **Step 5: Create `frontend/src/App.tsx`**

```tsx
import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import Items from './pages/Items'
import ItemDetail from './pages/ItemDetail'
import Jobs from './pages/Jobs'

export default function App() {
  return (
    <BrowserRouter>
      <nav style={{ padding: '12px 24px', borderBottom: '1px solid #e0e0e0', display: 'flex', gap: 24 }}>
        <NavLink to="/items" style={({ isActive }) => ({ fontWeight: isActive ? 'bold' : 'normal' })}>
          Catalogue
        </NavLink>
        <NavLink to="/jobs" style={({ isActive }) => ({ fontWeight: isActive ? 'bold' : 'normal' })}>
          Jobs
        </NavLink>
      </nav>
      <Routes>
        <Route path="/" element={<Items />} />
        <Route path="/items" element={<Items />} />
        <Route path="/items/:id" element={<ItemDetail />} />
        <Route path="/jobs" element={<Jobs />} />
      </Routes>
    </BrowserRouter>
  )
}
```

- [ ] **Step 6: Verify the frontend dev server starts**

```bash
cd frontend && npm run dev
```

Expected: Vite dev server running on `http://localhost:5173` without errors.

- [ ] **Step 7: Commit**

```bash
git add frontend/
git commit -m "feat(frontend): scaffold React + Vite + @semcore/ui + react-router-dom"
```

---

## Task 12: Types + API Client

**Files:**
- Create: `frontend/src/types/index.ts`
- Create: `frontend/src/api/client.ts`

- [ ] **Step 1: Create `frontend/src/types/index.ts`**

```ts
export type ItemType = 'game' | 'console' | 'accessory'
export type PriceCondition = 'loose' | 'cib' | 'new'
export type JobStatus = 'pending' | 'running' | 'done' | 'failed'

export interface Item {
  id: string
  external_id: string
  type: ItemType
  title: string
  platform: string
  region: string
  condition: string
  status: string
  cover_url: string
  created_at: string
  updated_at: string
}

export interface PriceSnapshot {
  id: string
  item_id: string
  source: string
  condition: PriceCondition
  price_min: number
  price_max: number
  price_median: number
  sample_count: number
  currency: string
  scraped_at: string
}

export interface ScrapeJob {
  id: string
  triggered_by: string
  status: JobStatus
  items_total: number
  items_done: number
  error?: string
  started_at: string
  finished_at?: string
}

// Convenience: latest prices for a single item, keyed by condition
export type LatestPrices = Record<PriceCondition, PriceSnapshot | undefined>
```

- [ ] **Step 2: Create `frontend/src/api/client.ts`**

```ts
import type { Item, PriceSnapshot, ScrapeJob } from '../types'

const BASE = '/api/v1'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  })
  if (!res.ok) throw new Error(`${init?.method ?? 'GET'} ${path} → ${res.status}`)
  return res.json() as Promise<T>
}

// Items
export const listItems = () => request<Item[]>('/items')
export const getItem = (id: string) => request<Item>(`/items/${id}`)
export const upsertItem = (item: Partial<Item>) =>
  request<Item>('/items', { method: 'POST', body: JSON.stringify(item) })
export const getItemPrices = (id: string) =>
  request<PriceSnapshot[]>(`/items/${id}/prices`)

// Prices
export const getLatestPrices = () => request<PriceSnapshot[]>('/prices/latest')

// Scrape
export const triggerFullScrape = () =>
  request<{ message: string }>('/scrape', { method: 'POST' })
export const triggerItemScrape = (id: string) =>
  request<{ message: string }>(`/scrape/items/${id}`, { method: 'POST' })
export const listJobs = () => request<ScrapeJob[]>('/scrape/jobs')
export const getJob = (id: string) => request<ScrapeJob>(`/scrape/jobs/${id}`)
```

- [ ] **Step 3: Verify TypeScript compilation**

```bash
cd frontend && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/ frontend/src/api/
git commit -m "feat(frontend): add TS types and typed API client"
```

---

## Task 13: Catalogue Page (Items)

**Files:**
- Create: `frontend/src/components/ItemsTable.tsx`
- Create: `frontend/src/pages/Items.tsx`

- [ ] **Step 1: Create `frontend/src/components/ItemsTable.tsx`**

```tsx
import DataTable from '@semcore/ui/data-table'
import Tag from '@semcore/ui/tag'
import { Item, PriceSnapshot, PriceCondition } from '../types'

interface Props {
  items: Item[]
  latestPrices: PriceSnapshot[]
  onSelect: (id: string) => void
}

function freshnessBadge(scrapedAt?: string) {
  if (!scrapedAt) return <Tag color="gray">—</Tag>
  const days = (Date.now() - new Date(scrapedAt).getTime()) / 86_400_000
  if (days < 7) return <Tag color="green">Récent</Tag>
  if (days < 30) return <Tag color="orange">&lt; 30j</Tag>
  return <Tag color="red">&gt; 30j</Tag>
}

function priceFor(snapshots: PriceSnapshot[], itemId: string, cond: PriceCondition) {
  const s = snapshots.find(s => s.item_id === itemId && s.condition === cond)
  return s ? `${s.price_median.toFixed(2)} €` : '—'
}

function latestDate(snapshots: PriceSnapshot[], itemId: string) {
  const dates = snapshots
    .filter(s => s.item_id === itemId)
    .map(s => s.scraped_at)
    .sort()
  return dates[dates.length - 1]
}

export default function ItemsTable({ items, latestPrices, onSelect }: Props) {
  const rows = items.map(item => ({
    id: item.id,
    title: item.title,
    platform: item.platform,
    type: item.type,
    status: item.status,
    loose: priceFor(latestPrices, item.id, 'loose'),
    cib: priceFor(latestPrices, item.id, 'cib'),
    new: priceFor(latestPrices, item.id, 'new'),
    freshness: latestDate(latestPrices, item.id),
  }))

  return (
    <DataTable data={rows} onRowClick={row => onSelect(row.id)}>
      <DataTable.Head>
        <DataTable.Column name="title" children="Titre" sortable />
        <DataTable.Column name="platform" children="Plateforme" sortable />
        <DataTable.Column name="type" children="Type" />
        <DataTable.Column name="status" children="Statut" />
        <DataTable.Column name="loose" children="Loose" />
        <DataTable.Column name="cib" children="CIB" />
        <DataTable.Column name="new" children="Neuf" />
        <DataTable.Column name="freshness" children="Fraîcheur" />
      </DataTable.Head>
      <DataTable.Body>
        <DataTable.Cell name="freshness">{(_, row) => freshnessBadge(row.freshness)}</DataTable.Cell>
      </DataTable.Body>
    </DataTable>
  )
}
```

- [ ] **Step 2: Create `frontend/src/pages/Items.tsx`**

```tsx
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Select from '@semcore/ui/select'
import Input from '@semcore/ui/input'
import { listItems, getLatestPrices } from '../api/client'
import ItemsTable from '../components/ItemsTable'
import type { Item, PriceSnapshot } from '../types'

export default function Items() {
  const navigate = useNavigate()
  const [items, setItems] = useState<Item[]>([])
  const [prices, setPrices] = useState<PriceSnapshot[]>([])
  const [typeFilter, setTypeFilter] = useState<string>('all')
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([listItems(), getLatestPrices()])
      .then(([i, p]) => { setItems(i); setPrices(p) })
      .finally(() => setLoading(false))
  }, [])

  const filtered = items.filter(item => {
    const matchType = typeFilter === 'all' || item.type === typeFilter
    const matchSearch = item.title.toLowerCase().includes(search.toLowerCase()) ||
      item.platform.toLowerCase().includes(search.toLowerCase())
    return matchType && matchSearch
  })

  return (
    <div style={{ padding: 24 }}>
      <h1>Catalogue</h1>
      <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
        <Input w={280}>
          <Input.Value
            placeholder="Rechercher titre ou plateforme…"
            value={search}
            onChange={setSearch}
          />
        </Input>
        <Select value={typeFilter} onChange={setTypeFilter}>
          <Select.Trigger />
          <Select.Popper>
            <Select.Option value="all">Tous</Select.Option>
            <Select.Option value="game">Jeux</Select.Option>
            <Select.Option value="console">Consoles</Select.Option>
            <Select.Option value="accessory">Accessoires</Select.Option>
          </Select.Popper>
        </Select>
      </div>
      {loading ? (
        <p>Chargement…</p>
      ) : (
        <ItemsTable
          items={filtered}
          latestPrices={prices}
          onSelect={id => navigate(`/items/${id}`)}
        />
      )}
    </div>
  )
}
```

- [ ] **Step 3: Verify TypeScript compilation**

```bash
cd frontend && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/
git commit -m "feat(frontend): add catalogue page with items table and filters"
```

---

## Task 14: Item Detail Page + Price Chart

**Files:**
- Create: `frontend/src/components/PriceChart.tsx`
- Create: `frontend/src/pages/ItemDetail.tsx`

- [ ] **Step 1: Create `frontend/src/components/PriceChart.tsx`**

```tsx
import { useMemo } from 'react'
import { Chart, Line, XAxis, YAxis, Tooltip, HoverLine, minMax } from '@semcore/d3-chart'
import { scaleLinear, scaleTime } from 'd3-scale'
import type { PriceSnapshot } from '../types'

interface Props {
  snapshots: PriceSnapshot[]
}

export default function PriceChart({ snapshots }: Props) {
  const data = useMemo(() => {
    // Group by scraped_at date (day), compute median per condition
    const byDate = new Map<string, Record<string, number>>()
    for (const s of snapshots) {
      const day = s.scraped_at.slice(0, 10)
      if (!byDate.has(day)) byDate.set(day, {})
      byDate.get(day)![s.condition] = s.price_median
    }
    return Array.from(byDate.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, prices]) => ({ date: new Date(date), ...prices }))
  }, [snapshots])

  if (data.length === 0) return <p>Aucune donnée de prix disponible.</p>

  const xScale = scaleTime().domain(minMax(data, 'date')).range([0, 500])
  const allValues = data.flatMap(d => [d.loose ?? 0, d.cib ?? 0, d.new ?? 0]).filter(Boolean)
  const yScale = scaleLinear().domain([0, Math.max(...allValues) * 1.1]).range([200, 0])

  return (
    <Chart data={data} scale={[xScale, yScale]} width={560} height={240} marginLeft={50} marginBottom={30}>
      <YAxis>
        <YAxis.Ticks />
        <YAxis.Grid />
      </YAxis>
      <XAxis>
        <XAxis.Ticks ticks={data.map(d => d.date)} />
      </XAxis>
      <HoverLine />
      <Tooltip>
        <Tooltip.Trigger />
        <Tooltip.Popper>
          {(_, { xIndex }) => {
            const d = data[xIndex]
            return d ? (
              <div>
                <div>{d.date.toLocaleDateString()}</div>
                {d.loose && <div>Loose: {d.loose.toFixed(2)} €</div>}
                {d.cib && <div>CIB: {d.cib.toFixed(2)} €</div>}
                {d.new && <div>Neuf: {d.new.toFixed(2)} €</div>}
              </div>
            ) : null
          }}
        </Tooltip.Popper>
      </Tooltip>
      <Line x="date" y="loose" color="#5899E2" />
      <Line x="date" y="cib" color="#F87E2A" />
      <Line x="date" y="new" color="#2DB940" />
    </Chart>
  )
}
```

- [ ] **Step 2: Create `frontend/src/pages/ItemDetail.tsx`**

```tsx
import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import Button from '@semcore/ui/button'
import Tag from '@semcore/ui/tag'
import DataTable from '@semcore/ui/data-table'
import { getItem, getItemPrices, triggerItemScrape } from '../api/client'
import PriceChart from '../components/PriceChart'
import type { Item, PriceSnapshot } from '../types'

export default function ItemDetail() {
  const { id } = useParams<{ id: string }>()
  const [item, setItem] = useState<Item | null>(null)
  const [snapshots, setSnapshots] = useState<PriceSnapshot[]>([])
  const [scraping, setScraping] = useState(false)

  useEffect(() => {
    if (!id) return
    Promise.all([getItem(id), getItemPrices(id)]).then(([i, p]) => {
      setItem(i)
      setSnapshots(p)
    })
  }, [id])

  async function handleScrape() {
    if (!id) return
    setScraping(true)
    await triggerItemScrape(id)
    setTimeout(async () => {
      const p = await getItemPrices(id)
      setSnapshots(p)
      setScraping(false)
    }, 3000)
  }

  if (!item) return <p style={{ padding: 24 }}>Chargement…</p>

  return (
    <div style={{ padding: 24 }}>
      <Link to="/items">← Retour</Link>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 16 }}>
        <div>
          <h1 style={{ margin: 0 }}>{item.title}</h1>
          <p style={{ margin: '4px 0 0', color: '#666' }}>{item.platform} · {item.type}</p>
        </div>
        <Button onClick={handleScrape} loading={scraping} use="primary">
          {scraping ? 'Scraping…' : 'Scraper maintenant'}
        </Button>
      </div>

      <h2>Historique des prix</h2>
      <div style={{ display: 'flex', gap: 16, marginBottom: 8 }}>
        <Tag color="blue">— Loose</Tag>
        <Tag color="orange">— CIB</Tag>
        <Tag color="green">— Neuf</Tag>
      </div>
      <PriceChart snapshots={snapshots} />

      <h2>Snapshots</h2>
      <DataTable data={snapshots}>
        <DataTable.Head>
          <DataTable.Column name="scraped_at" children="Date" />
          <DataTable.Column name="condition" children="Condition" />
          <DataTable.Column name="price_min" children="Min" />
          <DataTable.Column name="price_median" children="Médian" />
          <DataTable.Column name="price_max" children="Max" />
          <DataTable.Column name="sample_count" children="Annonces" />
        </DataTable.Head>
        <DataTable.Body>
          <DataTable.Cell name="scraped_at">
            {(_, row) => new Date(row.scraped_at).toLocaleString()}
          </DataTable.Cell>
          <DataTable.Cell name="price_min">{(_, row) => `${row.price_min.toFixed(2)} €`}</DataTable.Cell>
          <DataTable.Cell name="price_median">{(_, row) => `${row.price_median.toFixed(2)} €`}</DataTable.Cell>
          <DataTable.Cell name="price_max">{(_, row) => `${row.price_max.toFixed(2)} €`}</DataTable.Cell>
        </DataTable.Body>
      </DataTable>
    </div>
  )
}
```

- [ ] **Step 3: Install d3-scale if not included in @semcore/d3-chart**

```bash
cd frontend && npm install d3-scale @types/d3-scale
```

- [ ] **Step 4: Verify TypeScript compilation**

```bash
cd frontend && npx tsc --noEmit
```

Expected: no errors (fix any import issues if @semcore/d3-chart API differs — check `node_modules/@semcore/d3-chart/src` for exact export names).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/
git commit -m "feat(frontend): add item detail page with price history chart"
```

---

## Task 15: Jobs Page

**Files:**
- Create: `frontend/src/components/JobStatus.tsx`
- Create: `frontend/src/pages/Jobs.tsx`

- [ ] **Step 1: Create `frontend/src/components/JobStatus.tsx`**

```tsx
import Tag from '@semcore/ui/tag'
import ProgressBar from '@semcore/ui/progress-bar'
import type { ScrapeJob } from '../types'

const statusColor: Record<string, string> = {
  pending: 'gray',
  running: 'blue',
  done: 'green',
  failed: 'red',
}

interface Props {
  job: ScrapeJob
}

export default function JobStatus({ job }: Props) {
  const pct = job.items_total > 0 ? Math.round((job.items_done / job.items_total) * 100) : 0
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      <Tag color={statusColor[job.status] ?? 'gray'}>{job.status}</Tag>
      {job.status === 'running' && (
        <ProgressBar value={pct} size="s">
          <ProgressBar.Value />
        </ProgressBar>
      )}
      <span style={{ fontSize: 12, color: '#666' }}>
        {job.items_done}/{job.items_total} items
      </span>
    </div>
  )
}
```

- [ ] **Step 2: Create `frontend/src/pages/Jobs.tsx`**

```tsx
import { useEffect, useRef, useState } from 'react'
import Button from '@semcore/ui/button'
import DataTable from '@semcore/ui/data-table'
import { listJobs, triggerFullScrape } from '../api/client'
import JobStatus from '../components/JobStatus'
import type { ScrapeJob } from '../types'

export default function Jobs() {
  const [jobs, setJobs] = useState<ScrapeJob[]>([])
  const [triggering, setTriggering] = useState(false)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  async function fetchJobs() {
    const j = await listJobs()
    setJobs(j)
    return j
  }

  useEffect(() => {
    fetchJobs()
  }, [])

  // Auto-refresh every 5s when a job is running
  useEffect(() => {
    const hasRunning = jobs.some(j => j.status === 'running' || j.status === 'pending')
    if (hasRunning && !intervalRef.current) {
      intervalRef.current = setInterval(fetchJobs, 5000)
    } else if (!hasRunning && intervalRef.current) {
      clearInterval(intervalRef.current)
      intervalRef.current = null
    }
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [jobs])

  async function handleTrigger() {
    setTriggering(true)
    await triggerFullScrape()
    await fetchJobs()
    setTriggering(false)
  }

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ margin: 0 }}>Jobs de scraping</h1>
        <Button use="primary" onClick={handleTrigger} loading={triggering}>
          {triggering ? 'Démarrage…' : 'Lancer une collecte complète'}
        </Button>
      </div>
      <DataTable data={jobs}>
        <DataTable.Head>
          <DataTable.Column name="id" children="ID" />
          <DataTable.Column name="triggered_by" children="Déclencheur" />
          <DataTable.Column name="status" children="Statut" />
          <DataTable.Column name="started_at" children="Démarré le" />
          <DataTable.Column name="finished_at" children="Terminé le" />
          <DataTable.Column name="error" children="Erreur" />
        </DataTable.Head>
        <DataTable.Body>
          <DataTable.Cell name="status">{(_, row) => <JobStatus job={row as ScrapeJob} />}</DataTable.Cell>
          <DataTable.Cell name="started_at">
            {(_, row) => new Date(row.started_at).toLocaleString()}
          </DataTable.Cell>
          <DataTable.Cell name="finished_at">
            {(_, row) => row.finished_at ? new Date(row.finished_at).toLocaleString() : '—'}
          </DataTable.Cell>
          <DataTable.Cell name="error">
            {(_, row) => row.error || '—'}
          </DataTable.Cell>
        </DataTable.Body>
      </DataTable>
    </div>
  )
}
```

- [ ] **Step 3: Verify TypeScript compilation**

```bash
cd frontend && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 4: Run full dev stack and smoke-test the UI**

```bash
# Terminal 1
task dev:db

# Terminal 2
cd backend && air

# Terminal 3
cd frontend && npm run dev
```

Open `http://localhost:5173`:
- `/items` → catalogue page loads (empty table is fine)
- `/jobs` → jobs page with "Lancer une collecte complète" button
- POST an item via curl and refresh `/items` → item appears

- [ ] **Step 5: Final commit**

```bash
git add frontend/src/
git commit -m "feat(frontend): add jobs page with auto-refresh and scrape trigger"
```

---

## Summary

All 15 tasks produce a fully working TheVaultBO:

| Task | Deliverable |
|------|-------------|
| 1 | Root: Taskfile, Docker Compose, .env |
| 2 | Go domain models + price stats (tested) |
| 3 | PostgreSQL migrations (4 tables) |
| 4 | Store: DB connection + items store |
| 5 | Store: prices + jobs + settings |
| 6 | API: items handlers (tested) + chi router |
| 7 | API: prices + scrape handlers |
| 8 | eBay scraper with OAuth (tested) |
| 9 | Cron scheduler wiring eBay to DB |
| 10 | Server entrypoint (auto-migrate, wire all) |
| 11 | Frontend: React + Vite + @semcore/ui scaffold |
| 12 | Frontend: TypeScript types + API client |
| 13 | Frontend: Catalogue page with filters |
| 14 | Frontend: Item detail + price chart |
| 15 | Frontend: Jobs page + auto-refresh |
