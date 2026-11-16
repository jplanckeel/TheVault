plugins {
    alias(libs.plugins.android.application)
    // AGP 9 built-in Kotlin, enabled per module while gradle.properties keeps the global
    // opt-out required by ktlint / detekt. No kotlin-android plugin.
    alias(libs.plugins.android.built.`in`.kotlin)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

/** Number of commits on HEAD (1 when git is unavailable), used as the Android version code. */
val gitCommitCount: Provider<Int> =
    providers
        .exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText
        .map { it.trim().toIntOrNull() ?: 1 }

android {
    namespace = "com.thevault.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.thevault.app"
        minSdk = 24
        targetSdk = 35
        // Every build gets a higher code: VERSION_CODE in CI releases, else the commit count.
        versionCode =
            providers
                .environmentVariable("VERSION_CODE")
                .map(String::toInt)
                .orElse(gitCommitCount)
                .get()
        versionName = providers.environmentVariable("VERSION_NAME").orElse("1.0.0").get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.androidx.work.runtime)
    implementation(compose.runtime)
    implementation(compose.ui)

    debugImplementation(compose.uiTooling)
}
