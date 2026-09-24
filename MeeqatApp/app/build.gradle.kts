@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.meeqat.azan"
    compileSdk = 37

    signingConfigs {
        create("release") {
            val ciKeystore = file("release.keystore")
            val debugKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
            when {
                ciKeystore.exists() -> {
                    storeFile = ciKeystore
                    storePassword = System.getenv("KEYSTORE_PASSWORD") ?: (project.findProperty("MEEQAT_STORE_PASSWORD") as String? ?: "meeqat123")
                    keyAlias = System.getenv("KEY_ALIAS") ?: (project.findProperty("MEEQAT_KEY_ALIAS") as String? ?: "meeqat")
                    keyPassword = System.getenv("KEY_PASSWORD") ?: (project.findProperty("MEEQAT_KEY_PASSWORD") as String? ?: "meeqat123")
                }
                debugKeystore.exists() -> {
                    storeFile = debugKeystore
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
                else -> {
                    storeFile = debugKeystore
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }

    defaultConfig {
        applicationId = "com.meeqat.azan"
        minSdk = 26
        targetSdk = 37
        val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
        versionCode = System.getenv("MEEQAT_VERSION_CODE")?.toIntOrNull()
            ?: System.getenv("FLAMBO_VERSION_CODE")?.toIntOrNull()
            ?: runNumber?.let { 100000 + it } ?: 1
        versionName = System.getenv("MEEQAT_VERSION_NAME")
            ?: System.getenv("FLAMBO_VERSION_NAME") ?: "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

kotlin { jvmToolchain(21) }

dependencies {
    // Core
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.animation.core)
    implementation(libs.androidx.foundation)

    // Material 3 Expressive
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.adaptive.navigation)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material.icons.extended.android)

    // Navigation
    implementation(libs.navigation.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // WorkManager
    implementation(libs.workmanager)

    // Media3 (Azan playback)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    // Location
    implementation(libs.play.services.location)

    // Prayer calc + Hijri
    implementation(libs.adhan)
    implementation(libs.ummalqura)

    // Serialization (DataStore, prefs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.0")

    // Test
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.tooling.preview)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
