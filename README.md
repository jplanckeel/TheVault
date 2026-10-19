# TheVault

**Application mobile Kotlin Multiplatform, backend Go et back-office web pour gérer une collection de jeux vidéo, consoles et accessoires.**

## Structure du monorepo

| Dossier | Contenu |
|---|---|
| `composeApp/`, `shared/`, `android/` | Application mobile (Kotlin Multiplatform, Gradle) |
| `backend/` | API Go (PostgreSQL, scraping eBay, IGDB) — ex-TheVaultBO |
| `web/` | Back-office web React + Vite, servi par le backend en production |
| `design/stitch/` | Maquettes Stitch exportées |
| `docs/`, `scripts/` | Documentation et scripts d'import |

```bash
./gradlew assembleDebug   # app mobile
task dev                  # backend (Air) + web (Vite)
task test                 # tests Go
task docker:release       # image Docker backend + web
```

L'app mobile lit la clé API du backend dans `local.properties` (non versionné) :

```properties
thevault.api.key=<API_KEY du backend>
# optionnel, par défaut https://thevault.bananaops.cloud/api/v1
thevault.api.baseUrl=http://10.0.2.2:8080/api/v1
```

## Back-office (admin)

Le back-office est dans l'app web, section **Back-office** de la sidebar (visible uniquement des admins) :

- **Données** (`/admin/data`) : catalogue partagé avec détection des problèmes (doublons, sans plateforme, sans jaquette, sans IGDB, sans prix), édition des jeux / consoles / accessoires, fusion des doublons, suppression, relance IGDB ou du prix d'un objet.
- **Synchronisations** (`/admin/sync`) : journal des envois de l'app mobile et du web, des corrections admin, et tâches eBay / IGDB.

Le rôle admin est porté par le compte utilisateur : listez les e-mails dans `ADMIN_EMAILS` (séparés par des virgules) côté backend, puis connectez-vous normalement. Les corrections sont durables : un objet corrigé n'est plus écrasé par les synchros, un objet fusionné est redirigé vers celui conservé, et un objet supprimé est refusé (HTTP 410) s'il revient d'une synchro.

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
