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

The theme is **"The Radiant Obsidian Vault"** — dark mode only, defined in Stitch (Project ID `10534585831624995760`).

**Before implementing any screen**, call the `mcp_StitchMCP_get_screen` tool with the screen ID from `.agent/AGENTS.md`.

Design tokens live in `composeApp/src/commonMain/kotlin/com/thevault/ui/theme/`. Core rules:
- No `#FFFFFF` text → use `on_surface` (`#dee5ff`)
- No corners < 8dp, no 1px solid borders (use tonal surface elevation instead)
- Primary gradient CTAs: `#8455EF → #EC63FF` at 135°, corner 12dp
- NavBar glassmorphism: `surface_bright` at 60% opacity + 24dp blur

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
