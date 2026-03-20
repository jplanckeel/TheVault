---
description: Run code quality checks (lint, static analysis, tests)
---

# Code Quality Check Workflow

// turbo-all

## Steps

### 1. Kotlin formatting check
```bash
./gradlew ktlintCheck
```
- If formatting errors are found, auto-fix with:
```bash
./gradlew ktlintFormat
```

### 2. Static analysis with Detekt
```bash
./gradlew detekt
```
- Review reported issues in `build/reports/detekt/`.
- Fix all issues before proceeding (no `@Suppress` without justification comment).

### 3. Run unit tests
```bash
./gradlew :shared:testDebugUnitTest
./gradlew :composeApp:testDebugUnitTest
```

### 4. Check test coverage
```bash
./gradlew koverReport
```
- Open `build/reports/kover/html/index.html`.
- Domain + Data layers must be ≥ 80% coverage.

### 5. Build verification
```bash
./gradlew assembleDebug
```

### 6. Report
- All checks must pass before committing or creating a PR.
- If any check fails, fix the issue and re-run from step 1.
