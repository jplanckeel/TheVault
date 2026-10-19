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

Two themes, both dark mode only:

- **Mobile — « Nuit profonde, un seul violet »**: source of truth `design/nuit-profonde/README.md`, visual reference `design/nuit-profonde/refonte-mobile.html`. Tokens in `composeApp/src/commonMain/kotlin/com/thevault/ui/theme/` (`Color.kt`, `Badges.kt`, `Typography.kt`, `VaultRadius.kt`).
- **Web — « Premium Security »**: `design/DESIGN_SYSTEM.md`, tokens in `web/src/index.css` (`vault-*`). Not yet ported to « Nuit profonde ».

Mobile rules:
- No `#FFFFFF` text → use `OnSurface` (`#dee5ff`)
- Single accent: violet `Primary` (`#BA9EFF`) with `OnPrimary` text; badges are tinted per manufacturer / format (`Badges.kt`)
- Cards: `SurfaceContainerLow` + 1dp `CardBorderColor`, radius 18; buttons 52dp, radius 14
- Font: Plus Jakarta Sans only, bundled in `composeResources/font/`, tabular figures
- Reuse the shared components (`ui/component/`) instead of styling screens ad hoc
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
