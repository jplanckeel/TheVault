plugins {
    alias(libs.plugins.android.application)
    // AGP 9 built-in Kotlin, enabled per module while gradle.properties keeps the global
    // opt-out required by ktlint / detekt. No kotlin-android plugin.
    alias(libs.plugins.android.built.`in`.kotlin)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.thevault.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.thevault.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
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
