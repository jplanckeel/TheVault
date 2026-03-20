plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

// ─── Ktlint ────────────────────────────────────────────────────────────────
ktlint {
    version = "1.5.0"
    android = true
    outputToConsole = true
    ignoreFailures = false
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        include("**/*.kt")
        include("**/*.kts")
    }
}

// ─── Detekt ─────────────────────────────────────────────────────────────────
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
    source.setFrom(
        "composeApp/src/commonMain",
        "composeApp/src/androidMain",
        "shared/src/commonMain",
        "shared/src/androidMain",
    )
    parallel = true
    autoCorrect = false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
    }
    exclude("**/generated/**", "**/build/**")
}

// ─── Kover (coverage) ────────────────────────────────────────────────────────
kover {
    reports {
        total {
            html {
                onCheck = true
            }
            verify {
                onCheck = true
                rule {
                    minBound(80) // ≥ 80% coverage target
                }
            }
        }
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*.ComposableSingletons*",
                    "*_Factory*",
                    "*_HiltModules*",
                )
                packages(
                    "*.di",
                    "*.ui.preview",
                )
            }
        }
    }
}

// ─── Subproject configuration ─────────────────────────────────────────────────
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    ktlint {
        version = "1.5.0"
        android = true
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
            include("**/*.kt")
        }
    }
}
