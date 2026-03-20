# TheVault

**Application mobile Kotlin Multiplatform pour gérer une collection de jeux vidéo, consoles et accessoires.**

## Documentation Agent

Ce projet utilise des fichiers de configuration agent pour guider le développement assisté par IA :

- `.agent/AGENTS.md` — Contexte général, architecture, stack technique, règles de code
- `.agent/workflows/new_feature.md` — Implémenter une nouvelle feature (`/new_feature`)
- `.agent/workflows/init_project.md` — Initialiser le projet KMP (`/init_project`)
- `.agent/workflows/add_dependency.md` — Ajouter une dépendance KMP (`/add_dependency`)
- `.agent/workflows/check_quality.md` — Vérifier la qualité du code (`/check_quality`)

## Stack Technique

| Couche | Technologie |
|---|---|
| UI | Compose Multiplatform |
| Navigation | Decompose |
| DI | Koin |
| Async | Coroutines + Flow |
| BDD Locale | SQLDelight |
| Réseau | Ktor |
| Images | Coil 3 |
| Sérialisation | kotlinx.serialization |
| Date/Heure | kotlinx-datetime |
| Tests | kotlin.test, Turbine, MockK |
| Qualité | Detekt, Ktlint, Kover |

## Architecture

Le projet suit la **Clean Architecture** :

```
Domain → Data → Presentation
```

- **Domain** : Entities, UseCases, interfaces Repository (pur Kotlin)
- **Data** : RepositoryImpl, DataSources, DTOs, Mappers
- **Presentation** : ViewModels, UiState, UiEvent, Composables

## Design

Le design de l'application est défini dans **Stitch** (Google). Toujours consulter le MCP Stitch avant d'implémenter un écran.

## Qualité de code

- Ktlint pour le formatage
- Detekt pour l'analyse statique (config dans `config/detekt/detekt.yml`)
- Kover pour la couverture de tests (cible ≥ 80% sur domain + data)
- Conventional Commits pour les messages de commit

## Plateformes cibles

- **Android** : minSdk 24
- **iOS** : iOS 16+
