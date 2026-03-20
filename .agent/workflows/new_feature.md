---
description: Implement a new feature (screen, entity, or use case) in TheVault
---

# New Feature Workflow

## Prerequisites
- The feature is described by the user (entity type, screen, behavior).
- The design exists in Stitch (consult MCP before step 3).

---

## Steps

### 1. Understand the scope
- Ask the user to clarify the feature if the description is ambiguous.
- Identify which entity is involved: **Game**, **Console**, or **Accessory**.
- Identify which layer(s) are affected: domain, data, presentation.

### 2. Fetch the design from Stitch MCP
- Call `mcp_StitchMCP_get_screen` avec `projectId: 10534585831624995760` et le `screenId` correspondant :

| Écran à implémenter | screenId |
|---------------------|----------|
| Tableau de Bord | `c37bf5b35e6e42aaa21980add04510e8` |
| Ma Collection | `43009fc650ca4286ab82424968450048` |
| Mes Consoles | `1b5489ef9279461d96961a0e91e82e2c` |
| Mes Accessoires | `7b255bc3bf864c559657c3d7da3571db` |
| Détails du Jeu | `8af581b278a9441f958a78d63dab7b30` |
| Types de Jeux | `e2ec4f63935342b091099322682bb15c` |
| Suivi des Prix | `817d050faf41479b9dfb34b5d9b0b33a` |
| Ajouter un Item | `140a3c082b524cfebbc853fc3c55a338` |

- Extraire : couleurs, typographie, espacements, layout, interactions.
- Consulter `.agent/design/DESIGN_SYSTEM.md` pour les tokens de design.
- Ajouter les tokens manquants dans `composeApp/src/commonMain/ui/theme/`.

### 3. Create the domain layer (if needed)
- Define or update the `Entity` data class in `shared/src/commonMain/domain/model/`.
- Define the `Repository` interface in `shared/src/commonMain/domain/repository/`.
- Create a `UseCase` class in `shared/src/commonMain/domain/usecase/`.
  - Single `operator fun invoke()` function.
  - KDoc required.

### 4. Create the data layer (if needed)
- Create the `DTO` data class with `@Serializable` in `shared/src/commonMain/data/remote/dto/`.
- Create the `Mapper` extension function `fun FooDto.toDomain(): Foo` in `shared/src/commonMain/data/mapper/`.
- Create or update the `RepositoryImpl` in `shared/src/commonMain/data/repository/`.
- Update SQLDelight schema if local persistence is needed (`shared/src/commonMain/sqldelight/`).

### 5. Register dependencies in Koin
- Add new repository / data source bindings in `shared/src/commonMain/di/`.
- Add ViewModel binding in `composeApp/src/commonMain/di/`.

### 6. Create the presentation layer
- Create `UiState` and `UiEvent` sealed interfaces in `composeApp/src/commonMain/ui/<feature>/`.
- Create the `ViewModel` class injecting the relevant `UseCase`(s).
- Create the Screen composable `<FeatureName>Screen.kt` (stateless, receives state + callbacks).
- Create a `@Preview` function.

### 7. Add navigation
- Register the new screen in the navigation graph.
- Add navigation action from the originating screen.

### 8. Write tests
// turbo
- Run existing tests first:
  ```
  ./gradlew :shared:testDebugUnitTest
  ```
- Write unit tests for the new `UseCase`.
- Write unit tests for the new `ViewModel` using fakes.
- Write Compose UI tests for critical interactions.

### 9. Verify build
// turbo
```
./gradlew assembleDebug
```

### 10. Commit
- Follow Conventional Commits format.
- Stage only files related to the feature.
```
git add -p
git commit -m "feat(<scope>): <short description>"
```
