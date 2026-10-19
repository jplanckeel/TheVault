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
./gradlew :shared:testDebugUnitTest
./gradlew :composeApp:testDebugUnitTest

# Run a single test class
./gradlew :shared:testDebugUnitTest --tests "com.thevault.domain.usecase.SaveGameUseCaseTest"

# Test coverage report (open build/reports/kover/html/index.html)
./gradlew koverReport

# Build debug APK
./gradlew assembleDebug
```

## Monorepo layout

- **Mobile app** — Gradle project at the root (`composeApp/`, `shared/`, `android/`). Everything below in this section applies to it.
- **`backend/`** — Go API (module `github.com/jplanckeel/thevaultbo`), PostgreSQL migrations in `backend/migrations/`. Commands: `task dev:backend`, `task test`, `task lint`.
- **`web/`** — React + Vite + Tailwind back-office, served by the backend via `STATIC_DIR` in production. Commands: `task dev:web`, `cd web && npm run build`. See `web/DESIGN.md` and `.kiro/PROJECT.md`.
- **`Dockerfile` / `docker-compose.yml`** build and deploy backend + web only (image `thevaultbo`).
- **Back-office** — web only (`web/src/pages/admin/`, API `/api/v1/admin/*`). Access requires `users.is_admin`, granted through the `ADMIN_EMAILS` env var and checked in DB by `AdminMiddleware`. Every catalogue write is logged in `sync_events`; admin edits set `items.admin_edited` (sync no longer overwrites catalogue fields), merges add `item_aliases`, deletes add `item_tombstones` (sync gets 410).

The mobile app talks to the backend through `shared/.../data/remote/TheVaultBo*DataSource.kt`. The API key and base URL come from `local.properties` (`thevault.api.key`, `thevault.api.baseUrl`) via the generated `com.thevault.config.ApiConfig` — never hardcode them.

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
  │   ├── remote/       # Ktor data sources (IGDB, RAWG APIs)
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

The theme is **"Premium Security"** — dark mode only, **shared by the mobile app and the web app**. The single source of truth is `design/DESIGN_SYSTEM.md` (tokens, typography, components, navigation); visual references are the Stitch mockups in `design/stitch/*/screen.png`.

Tokens are implemented twice and must stay in sync: `web/src/index.css` (`vault-*`) and `composeApp/src/commonMain/kotlin/com/thevault/ui/theme/`. Core rules:
- No `#FFFFFF` text → use `OnSurface` / `vault-on-surface` (`#e4e1e6`)
- Single accent: lavender `Primary` (`#d8b9ff`), filled CTAs — no violet→magenta gradient
- No corners < 8dp except pills; cards use `surface-low` + `outline-variant` border at 30 %
- Fonts: Montserrat (titles, figures) + Inter (text), bundled in `composeResources/font/`
- Reuse the shared components (`ui/component/` ↔ `web/src/components/ui/`) instead of styling screens ad hoc
- Never display invented figures: unknown values render as `—`

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
