---
description: Add or update a dependency in the project
---

# Add Dependency Workflow

## Steps

### 1. Verify KMP compatibility
- Check that the library supports Kotlin Multiplatform (commonMain, androidMain, iosMain).
- Consult [https://klibs.io](https://klibs.io) or the library's README for KMP support status.
- Confirm the library supports the minimum SDK targets:
  - Android: minSdk 24
  - iOS: iOS 16+

### 2. Add to version catalog
- Open `gradle/libs.versions.toml`.
- Add the version under `[versions]`.
- Add the artifact under `[libraries]`.
- If it's a Gradle plugin, add it under `[plugins]`.

Example:
```toml
[versions]
ktor = "3.1.0"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-android = { module = "io.ktor:ktor-client-android", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
```

### 3. Add to build.gradle.kts
- In the relevant module's `build.gradle.kts`, add the dependency using the catalog alias.
- Use `commonMainImplementation` for shared KMP code.
- Use `androidMainImplementation` / `iosMainImplementation` for platform-specific code.

Example:
```kotlin
commonMain.dependencies {
    implementation(libs.ktor.client.core)
}
androidMain.dependencies {
    implementation(libs.ktor.client.android)
}
iosMain.dependencies {
    implementation(libs.ktor.client.darwin)
}
```

### 4. Sync and verify
// turbo
```bash
./gradlew :shared:build
```

### 5. Update Podfile if iOS native library is required
- Run `pod install` in `iosApp/`.
