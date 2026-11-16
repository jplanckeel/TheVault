# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Format Kotlin code
./gradlew ktlintFormat

# Check formatting (no auto-fix)
./gradlew ktlintCheck

# Static analysis
./gradlew detekt

# Run unit tests
./gradlew :shared:testAndroidHostTest
./gradlew :composeApp:testAndroidHostTest

# Run a single test class
./gradlew :shared:testAndroidHostTest --tests "com.thevault.domain.usecase.SaveGameUseCaseTest"

# Test coverage report (open build/reports/kover/html/index.html)
./gradlew koverReport

# Build debug APK (androidApp/build/outputs/apk/debug/)
./gradlew :androidApp:assembleDebug
```

## Monorepo layout

- **Mobile app** — Gradle project at the root: `androidApp/` (Android entry point: Activity, Application, manifest, launcher icons), `composeApp/` (shared Compose UI, KMP library) and `shared/` (domain + data, KMP library). AGP 9 forbids `com.android.application` in a KMP module, so KMP modules use `com.android.kotlin.multiplatform.library` and are configured in `kotlin { android { } }`. Run configuration: `androidApp` (`.run/androidApp.run.xml`).
- **`backend/`** — Go API (module `github.com/jplanckeel/thevaultbo`), PostgreSQL migrations in `backend/migrations/`. Commands: `task dev:backend`, `task test`, `task lint`.
- **`web/`** — React + Vite + Tailwind back-office, served by the backend via `STATIC_DIR` in production. Commands: `task dev:web`, `cd web && npm run build`. See `web/DESIGN.md` and `.kiro/PROJECT.md`.
- **`Dockerfile` / `docker-compose.yml`** build and deploy backend + web only (image `thevaultbo`).
- **Security** — `JWT_SECRET` (≥ 32 chars) is mandatory, never add a fallback secret. Validate user input in `internal/domain/validation.go`, keep the rate limits / body limits / security headers of `internal/api/security.go`, and keep `govulncheck`, `gosec` and `npm audit` green.
- **Back-office** — web only (`web/src/pages/admin/`, API `/api/v1/admin/*`). Access requires `users.is_admin`, granted through the `ADMIN_EMAILS` env var and checked in DB by `AdminMiddleware`. Every catalogue write is logged in `sync_events`; admin edits set `items.admin_edited` (sync no longer overwrites catalogue fields), merges add `item_aliases`, deletes add `item_tombstones` (sync gets 410).

The mobile app talks to the backend through `shared/.../data/remote/TheVaultBo*DataSource.kt` (IGDB search proxy, catalogue sync, catalogue sheets `/items/lookup` (cover, genre, year, studio — applied on start and from Réglages), eBay quotes `/prices/quotes` + `/prices/history`, account `/auth/login`, `/backup`, `/shares`; public pages `/share/{token}` read `/public/shares/{token}`). The API key and base URL come from `local.properties` (`thevault.api.key`, `thevault.api.baseUrl`) via the generated `com.thevault.config.ApiConfig` — never hardcode them.

## Architecture

This is a **Kotlin Multiplatform** project targeting Android and iOS with two Gradle modules:

- **`shared/`** — Domain + Data layers (pure Kotlin, no UI)
- **`composeApp/`** — Presentation layer (Compose Multiplatform, ViewModels)

### Clean Architecture layers

```
composeApp/commonMain
  └── ui/
      ├── screen/       # Composable screens (<ScreenName>Screen.kt)
      ├── viewmodel/    # ViewModels with UiState + UiEvent
      └── theme/        # Color, Typography, Shape, Spacing

shared/commonMain
  ├── domain/
  │   ├── entity/       # Data classes (Game, Console, Accessory, VaultStats)
  │   ├── usecase/      # One invoke() per UseCase
  │   └── repository/   # Interfaces only
  ├── data/
  │   ├── repository/   # RepositoryImpl
  │   ├── local/        # SQLDelight data sources
  │   ├── remote/       # Ktor data sources (back-office: search, sync, quotes, account / backup / shares)
  │   ├── dto/          # @Serializable data classes
  │   └── mapper/       # Extension functions: fun FooDto.toDomain(): Foo
  └── di/               # Koin modules (SharedModule.kt)
```

### Key rules

- **Domain layer** has zero framework dependencies — pure Kotlin only
- **ViewModels** must not import any Android-specific class
- **DTOs** must have `@Serializable`; mappers must be extension functions
- **Composable screens** are stateless — receive state + callbacks, no ViewModel reference
- **UseCases** have a single `operator fun invoke()`
- No `!!` operator — use safe calls, `requireNotNull`, or sealed classes
- Max 300 lines per file, max 40 lines per function
- All public declarations require KDoc

## Design System

- **Mobile — « console »** (deux coques : *Coque grise* claire, *Black Edition* sombre, au choix dans Réglages) : source of truth `design/console/README.md`, visual reference `design/console/maquettes-app-mobile.html`. Tokens in `composeApp/src/commonMain/kotlin/com/thevault/ui/theme/` (`Color.kt`, `Typography.kt`, `VaultRadius.kt`, `Surfaces.kt`), read through `Vault.colors` / `Vault.type`.
- **Web — « console »** (same design system as mobile): tokens and `tv-*` component classes in `web/src/index.css` (two shells via `data-theme`, picker in the sidebar), components in `web/src/components/ui/`.

Mobile rules:
- Never hardcode a color in a screen: use `Vault.colors.*` so both shells work
- Single action color: `indigo` (« bouton A ») with `onIndigo`; secondary actions in `lavender`; platform bands from `Manufacturer.color`
- Surfaces: `Panel` / `Modifier.molded` (radius 22) on `shell`, recessed `Modifier.well` for selectors and search; buttons are pills
- Fonts: Archivo (display, Expanded for engraved labels), IBM Plex Sans (text), IBM Plex Mono (prices) — bundled in `composeResources/font/`
- Reuse the shared components (`ui/component/`) instead of styling screens ad hoc
- Never display invented figures: unknown values render as `—`; values come from eBay quotes (backend) or what the user typed
- Each copy is described by `Components` (media / box / manual / extras, grade per part, sealed, digital); completeness (Loose / CIB / Neuf) is always derived, never typed
- Value of a copy: reliable PAL eBay quote (`ebay_pal`) → value typed by the user → Gameye US price (`gameye_import_us`, always shown as « ≈ … cote US indicative »)
- The collection is PAL: the Gameye `Country` column is ignored on import

## Dependency Management

All dependencies are declared in `gradle/libs.versions.toml`. Before adding a dependency, verify it supports KMP. Add the version to the `[versions]` section, the library to `[libraries]`, and plugins to `[plugins]`.

## Code Quality Thresholds

- Detekt config: `config/detekt/detekt.yml`
- Kover coverage target: **≥ 80%** on `shared/domain` and `shared/data`
- Excluded from coverage: generated code, DI modules, Preview composables

## Git Conventions

Branch naming: `feat/<ticket>-description`, `fix/<ticket>-description`, `refactor/...`, `chore/...`

Commit format (Conventional Commits):
```
feat(game): add wishlist management
fix(sync): resolve duplicate entries on first sync
chore(deps): update ktor to 3.1.0
```
