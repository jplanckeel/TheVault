# TheVault — Agent Configuration

## Project Overview

**TheVault** is a Kotlin Multiplatform (KMP) mobile application for managing a personal video game collection: games, consoles, and accessories.

- **Platforms**: Android & iOS
- **Language**: Kotlin (Multiplatform)
- **UI Framework**: Compose Multiplatform
- **Design Source**: Stitch (Google) — always consult the MCP Stitch server for UI specifications before implementing screens

---

## Architecture

This project follows **Clean Architecture** with the following layers:

```
TheVault/
├── composeApp/             # Shared UI (Compose Multiplatform)
│   └── src/
│       ├── commonMain/     # Shared Compose UI + ViewModels
│       ├── androidMain/    # Android-specific
│       └── iosMain/        # iOS-specific
├── shared/                 # Shared business logic
│   └── src/
│       ├── commonMain/
│       │   ├── domain/     # Entities, UseCases, Repository interfaces
│       │   ├── data/       # Repository implementations, data sources
│       │   └── di/         # Dependency injection modules
│       ├── androidMain/
│       └── iosMain/
└── iosApp/                 # iOS entry point (Swift)
```

### Layer Rules
- **Domain layer**: pure Kotlin, no framework dependency. Contains `Entity`, `UseCase`, `Repository` interfaces.
- **Data layer**: implements repository interfaces. Contains `RepositoryImpl`, `LocalDataSource`, `RemoteDataSource`, `DTO`, `Mapper`.
- **Presentation layer**: `ViewModel` (using `ViewModel` from `lifecycle-viewmodel`), `UiState`, `UiEvent`, Compose screens.

---

## Domain Model

The application manages three main entity types:

### Game
- title, platform, genre, releaseYear, region, condition, coverUrl
- status: OWNED | WISHLIST | SOLD | LOANED

### Console
- name, manufacturer, region, color, condition, productionYear
- status: OWNED | SOLD

### Accessory
- name, type, compatibleConsole, condition
- status: OWNED | SOLD

---

## Technology Stack

| Layer | Technology |
|---|---|
| UI | Compose Multiplatform |
| Navigation | Decompose or Navigation Compose |
| DI | Koin |
| Async | Coroutines + Flow |
| Local DB | SQLDelight |
| Networking | Ktor |
| Image Loading | Coil 3 (multiplatform) |
| Serialization | kotlinx.serialization |
| Date/Time | kotlinx-datetime |
| Testing | kotlin.test, Turbine, MockK |
| Build | Gradle (version catalogs) |

---

## Design System

> Source : Stitch MCP — Project ID `10534585831624995760`  
> Détails complets : `.agent/design/DESIGN_SYSTEM.md`

### Thème : "The Radiant Obsidian Vault"
- **Mode** : Dark uniquement
- **Police** : Plus Jakarta Sans (Display, Headline, Body, Label)
- **Coins** : 8dp minimum (`DEFAULT`), jamais de corners 90°
- **Device** : Mobile (390px de largeur)

### Couleurs clés (tokens Compose)

| Token | Valeur | Usage |
|-------|--------|-------|
| `surface` / `background` | `#060e20` | Fond principal |
| `surface_container_low` | `#091328` | Sections principales |
| `surface_container` | `#0f1930` | Cards interactives |
| `surface_container_highest` | `#192540` | Pop-overs flottants |
| `surface_bright` | `#1f2b49` | NavBar glassmorphique |
| `primary` | `#ba9eff` | Accents, couleur signature |
| `primary_dim` | `#8455ef` | Début gradient CTA |
| `tertiary` | `#ec63ff` | Fin gradient CTA, heat nodes |
| `secondary_container` | `#444173` | Fond des Status Chips |
| `on_surface` | `#dee5ff` | Texte principal |
| `on_surface_variant` | `#a3aac4` | Texte secondaire/metadata |

### Règle CTA (gradient principal)
```kotlin
// 135° : primary_dim → tertiary
Brush.linearGradient(listOf(Color(0xFF8455EF), Color(0xFFEC63FF)))
cornerRadius = 12.dp
```

### Règle glassmorphism (NavBar)
```kotlin
background = Color(0x991F2B49)  // surface_bright @ 60%
backdropBlur = 24.dp
```

### Règle "No-Line"
**Interdit** : bordures 1px solides pour sectionner.  
**Obligatoire** : utiliser les décalages de teinte surface (tonal carving).

### Règles absolues
- ❌ Jamais `#FFFFFF` pour le texte → utiliser `on_surface` `#dee5ff`
- ❌ Jamais de corners < 8dp
- ❌ Jamais de dividers standards
- ✅ Spacing-16/20 entre sections majeures
- ✅ Art du jeu peut déborder des cards (effet premium)

### Écrans du projet Stitch

| Écran | Screen ID Stitch | Description |
|-------|-----------------|-------------|
| **Tableau de Bord** | `c37bf5b35e6e42aaa21980add04510e8` | Dashboard, stats collection |
| **Ma Collection** | `43009fc650ca4286ab82424968450048` | Liste des jeux |
| **Mes Consoles** | `1b5489ef9279461d96961a0e91e82e2c` | Liste des consoles |
| **Mes Accessoires** | `7b255bc3bf864c559657c3d7da3571db` | Liste des accessoires |
| **Détails du Jeu** | `8af581b278a9441f958a78d63dab7b30` | Fiche détail jeu |
| **Types de Jeux** | `e2ec4f63935342b091099322682bb15c` | Catégories/genres |
| **Suivi des Prix** | `817d050faf41479b9dfb34b5d9b0b33a` | Tracker de valeur |
| **Ajouter un Item** | `140a3c082b524cfebbc853fc3c55a338` | Formulaire d'ajout |

- **Toujours** appeler `mcp_StitchMCP_get_screen` avec `projectId: 10534585831624995760` avant d'implémenter un écran.
- Les design tokens vont dans `composeApp/src/commonMain/ui/theme/`.

---

## Code Quality Standards

### General
- All public declarations **must** have KDoc documentation.
- No `!!` (non-null assertion) usage — use safe calls, `requireNotNull`, or sealed classes.
- No raw string literals for user-visible text — use string resources.
- Max file length: **300 lines**. Split into smaller files if exceeded.
- Max function length: **40 lines**.

### Architecture Rules
- ViewModels **must not** import Android-specific classes.
- UseCases are single-responsibility: one `invoke` operator function.
- Repositories are always defined as interfaces in the domain layer.
- Data classes in DTO layer must have `@Serializable`.
- Mappers must be extension functions: `fun FooDto.toDomain(): Foo`.

### Kotlin Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Prefer `val` over `var`.
- Prefer expression bodies for simple functions.
- Use `sealed interface` for UI states and events.
- Use `data class` for entities and DTOs.
- Use `object` for singletons and mappers where appropriate.

### Compose Rules
- Each screen has its own file named `<ScreenName>Screen.kt`.
- Stateless composable functions receive state + callbacks (no ViewModel directly).
- Composable functions preview must be provided with `@Preview`.
- `@Composable` functions must be named in PascalCase.

### Testing
- Each UseCase must have unit tests.
- Each ViewModel must have unit tests with fake repositories.
- UI tests use Compose testing APIs.
- Test coverage target: **≥ 80%** for domain and data layers.

---

## Git Conventions

### Branch Naming
```
feat/<ticket>-short-description
fix/<ticket>-short-description
refactor/<ticket>-short-description
chore/<ticket>-short-description
```

### Commit Messages (Conventional Commits)
```
feat(game): add wishlist management
fix(sync): resolve duplicate entries on first sync
refactor(collection): extract pagination logic to use case
chore(deps): update ktor to 3.1.0
```

---

## Instructions for the AI Agent

1. **Before implementing any screen**: consult the `stitch` MCP server to fetch the exact design specification.
2. **Before adding a dependency**: check that it supports Kotlin Multiplatform (KMP) and is listed in the `libs.versions.toml` catalog.
3. **Always generate tests** alongside implementations (never skip).
4. **Never break existing tests** — run `./gradlew :shared:testDebugUnitTest` before proposing a PR.
5. **Ask before architectural decisions** that are not covered by this document.
6. **Prefer existing patterns** in the codebase over introducing new ones.
7. **Use workflows** defined in `.agent/workflows/` for repetitive tasks.
