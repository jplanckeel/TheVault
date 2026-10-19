import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
}

// ─── API config (secrets read from local.properties or env, never committed) ──
val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

fun configValue(
    key: String,
    env: String,
    default: String,
): String = localProperties.getProperty(key) ?: System.getenv(env) ?: default

val apiBaseUrl = configValue("thevault.api.baseUrl", "THEVAULT_API_BASE_URL", "https://thevault.bananaops.cloud/api/v1")
val apiKey = configValue("thevault.api.key", "THEVAULT_API_KEY", "")

val generateApiConfig =
    tasks.register("generateApiConfig") {
        val outputDir = layout.buildDirectory.dir("generated/apiConfig/commonMain/kotlin")
        val baseUrlValue = apiBaseUrl
        val apiKeyValue = apiKey
        inputs.property("apiBaseUrl", baseUrlValue)
        inputs.property("apiKey", apiKeyValue)
        outputs.dir(outputDir)
        doLast {
            val file = outputDir.get().file("com/thevault/config/ApiConfig.kt").asFile
            file.parentFile.mkdirs()
            file.writeText(
                """
                |package com.thevault.config
                |
                |/** Generated from local.properties / environment. Do not edit. */
                |internal object ApiConfig {
                |    const val BASE_URL: String = "$baseUrlValue"
                |    const val API_KEY: String = "$apiKeyValue"
                |}
                |
                """.trimMargin(),
            )
        }
    }

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateApiConfig.map { it.outputs.files.singleFile })
        }

        commonMain.dependencies {
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // DateTime
            implementation(libs.kotlinx.datetime)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)

            // Koin
            implementation(libs.koin.core)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.koin.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.koin.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

android {
    namespace = "com.thevault.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("TheVaultDatabase") {
            packageName.set("com.thevault.database")
        }
    }
}
