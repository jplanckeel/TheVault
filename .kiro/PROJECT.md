# TheVault — Project Context

You are working on **TheVault**, a video game collection management application with price tracking and IGDB metadata enrichment.

## Project Overview

TheVault is a personal gaming collection tracker that helps users:
- Catalogue their video games, consoles, and accessories
- Enrich item metadata automatically from IGDB (covers, summaries, genres, release year, etc.)
- Track market prices over time by scraping eBay sold listings
- Manage personal collections and wishlists per user
- Browse a public catalogue of games and consoles

## Architecture

The project is split into three components:

### 1. Backend (`backend/`) — Go
REST API written in Go (1.25+) using the **chi** router and **pgx/v5** for PostgreSQL access.

Key directories:
- `backend/cmd/server/` — entry point (`main.go`)
- `backend/internal/api/` — chi router, middlewares (admin auth, user auth)
- `backend/internal/api/handlers/` — HTTP handlers (items, prices, consoles, accessories, scrape jobs, IGDB sync, user auth, user collections)
- `backend/internal/store/` — PostgreSQL persistence layer (one file per aggregate)
- `backend/internal/domain/` — domain models (`Item`, `PriceSnapshot`, `ScrapeJob`, etc.)
- `backend/internal/igdb/` — IGDB API client and sync runner
- `backend/internal/scraper/` — eBay scraper (Browse API, OAuth client credentials)
- `backend/internal/scheduler/` — cron-based scheduler (robfig/cron) for periodic IGDB sync and eBay scraping
- `backend/migrations/` — golang-migrate SQL migrations (embedded via `embed.go`)

Authentication:
- **Admin auth** (`AuthMiddleware`): JWT or `X-API-Key` header. Used for admin operations (trigger scrape, edit covers, sync IGDB) and the mobile app.
- **User auth** (`UserAuthMiddleware`): JWT only, extracts `user_id` (sub) into `r.Context()` via the `handlers.UserIDKey` key. Used for user collection / wishlist operations.

Key environment variables: `DATABASE_URL`, `PORT`, `JWT_SECRET`, `ADMIN_PASSWORD`, `API_KEY`, `EBAY_CLIENT_ID`, `EBAY_CLIENT_SECRET`, `IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET`, `EBAY_CRON`, `IGDB_CRON`, `STATIC_DIR`.

### 2. Frontend (`frontend/`) — React + Vite + TypeScript
Single-page app served by the Go backend in production (`STATIC_DIR=./frontend/dist`).

Stack:
- React 18 + TypeScript + Vite 8
- React Router 7
- Tailwind CSS 4 (theme defined directly in `frontend/src/index.css` via `@theme`)
- Recharts for price history visualization

Key files:
- `frontend/src/App.tsx` — routes, layout, sidebar, search bar
- `frontend/src/AuthContext.tsx` — user + admin auth state, `login` / `register` / `adminLogin` / `logout`
- `frontend/src/api.ts` — typed API client (`api.items`, `api.collection`, `api.wishlist`, `api.scrape`, etc.)
- `frontend/src/types.ts` — shared TS types (`Item`, `PriceSnapshot`, `ScrapeJob`)
- `frontend/src/pages/` — route components

Design system:
- Theme tokens: `vault-bg`, `vault-surface*`, `vault-primary` (purple `#BA9EFF`), `vault-tertiary` (`#EC63FF`), `vault-on-surface*`, `vault-error`, `vault-success`
- Logo: hexagonal violet/indigo gradient (`#9333ea` → `#4f46e5`)
- Navigation: sidebar with "Mon espace" (Dashboard, Collection, Wishlist), "Explorer" (Catalogue, Consoles, Accessoires), "Admin" (Jobs)

### 3. Mobile app (separate repo)
A companion mobile app exists in a separate repository. It pushes the user's collection to the backend through `POST /api/v1/items` with the `X-API-Key` header. The mobile app and the web frontend share the same backend.

## Data Model

Core tables (PostgreSQL, UUIDs as PKs):
- `items` — games, consoles, accessories. One global catalogue (each item stored once, `external_id` unique). Includes IGDB enrichment fields (`igdb_id`, `summary`, `genres`, `developer`, `publisher`, `release_year`, `cover_url`).
- `price_snapshots` — eBay scraping results per item × condition (`loose`, `cib`, `new`), with `price_min`, `price_max`, `price_median`, `sample_count`, `currency`, `scraped_at`.
- `scrape_jobs` — tracks scraping/sync runs (cron or manual): `job_type`, `triggered_by`, `status`, `items_total`, `items_done`.
- `settings` — key-value store for runtime configuration.
- `users` — registered web users (email + bcrypt password hash + name).
- `user_collections` — many-to-many between users and items, with `status` (`owned` or `wishlist`) and `added_at`. Composite PK `(user_id, item_id)`.

**Important**: items are stored once globally. Each user attaches existing items to their collection or wishlist via `user_collections`.

## API Routes

Mounted under `/api/v1`.

**Public** (no auth):
- `POST /auth/register`, `POST /auth/login` — user signup / login (returns JWT)
- `POST /login` — admin login (password-only, returns admin JWT)
- `GET /items`, `GET /items/{id}`, `GET /items/{id}/prices`
- `GET /consoles`, `GET /consoles/{id}`, `GET /consoles/{id}/games`
- `GET /accessories`, `GET /accessories/{id}`
- `GET /prices/latest`

**User-authenticated** (`UserAuthMiddleware`):
- `GET /auth/me`
- `GET /collection`, `POST /collection`, `DELETE /collection/{itemId}`
- `GET /wishlist` (filtered view of `user_collections` where `status='wishlist'`)

**Admin-authenticated** (`AuthMiddleware`):
- `GET /search` (IGDB search)
- `POST /items` (upsert from mobile), `POST /items/sync-igdb`, `PUT /items/{id}/cover`
- `POST /scrape`, `GET /scrape`, `GET /scrape/{id}`

## External Integrations

- **IGDB** (Twitch developer API): metadata enrichment. Auth via OAuth2 client credentials. Client in `backend/internal/igdb/client.go`. Searches by title, fetches cover, summary, genres, developer/publisher, release year. The `Runner` enriches items missing `igdb_id`.
- **eBay Browse API**: price scraping. OAuth2 client credentials. Client in `backend/internal/scraper/ebay.go`. Searches sold listings per item × condition, computes min/max/median, persists snapshots.

## Tooling & Conventions

- **Build**: `cd backend && go build ./...` and `cd frontend && npm run build`
- **Tests**: `cd backend && go test ./...` (Go testing). No frontend test runner configured.
- **Migrations**: golang-migrate, embedded via `migrations/embed.go`. Run automatically on server startup. New migrations follow `NNNNNN_name.up.sql` / `NNNNNN_name.down.sql`.
- **Task runner**: `Taskfile.yml` for common operations.
- **Docker**: `docker-compose.yml` runs Postgres + backend.
- **Code style**: standard Go fmt; TypeScript strict mode.
- **Languages in conversation**: respond in French to match project audience.

## Common Tasks

When working on this project:
1. **New API endpoint**: add handler in `backend/internal/api/handlers/`, wire it in `backend/internal/api/router.go`, then expose it in `frontend/src/api.ts`.
2. **New DB column**: create matching `up`/`down` migration files, update the domain model in `backend/internal/domain/models.go`, update the store's scan/upsert SQL, update the TS type in `frontend/src/types.ts`.
3. **New page**: create component in `frontend/src/pages/`, add a `<Route>` in `frontend/src/App.tsx`. Use existing `vault-*` theme tokens for styling.
4. **Verify after changes**: always run `go build ./...` (backend) and `npm run build` (frontend) before declaring done.
