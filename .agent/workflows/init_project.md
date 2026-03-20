---
description: Initialize the Kotlin Multiplatform project structure from scratch
---

# Project Initialization Workflow

// turbo-all

## Steps

### 1. Create the KMP project with KMP Wizard template
```bash
# From the project root directory
cd /home/jplanckeel/git_public/jplanckeel/TheVault
```

### 2. Verify Kotlin & Gradle versions
- Kotlin: must be ≥ 2.0.0
- Gradle: must be ≥ 8.6
- AGP (Android Gradle Plugin): must be ≥ 8.3

### 3. Configure version catalog
- Ensure `gradle/libs.versions.toml` exists with all dependencies defined.
- All dependency versions must be centralized here — no hardcoded versions in `build.gradle.kts` files.

### 4. Setup code quality tools
- Add **Ktlint** plugin for Kotlin code formatting.
- Add **Detekt** for static analysis.
- Configure Detekt with the project ruleset at `config/detekt/detekt.yml`.

### 5. Create base project structure
Create the following directories manually if not generated:
```
shared/src/commonMain/kotlin/com/thevault/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── data/
│   ├── local/
│   ├── remote/
│   │   └── dto/
│   ├── repository/
│   └── mapper/
└── di/

composeApp/src/commonMain/kotlin/com/thevault/
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Typography.kt
│   │   ├── Shape.kt
│   │   └── Theme.kt
│   ├── component/
│   └── navigation/
└── di/
```

### 6. Setup Koin
- Add Koin KMP dependency to `shared` and `composeApp`.
- Create `AppModule.kt` in each `di/` directory.
- Initialize Koin in the Android `Application` class and iOS entry point.

### 7. Setup SQLDelight
- Add SQLDelight plugin and dependency.
- Create initial schema in `shared/src/commonMain/sqldelight/com/thevault/database/TheVault.sq`.

### 8. Verify the build
```bash
./gradlew assembleDebug
```

### 9. Run initial lint checks
```bash
./gradlew ktlintCheck
./gradlew detekt
```
