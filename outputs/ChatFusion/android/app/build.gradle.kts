plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.yogesh.multiaichat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yogesh.multiaichat"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.4"

        val apiBaseUrl = providers.gradleProperty("API_BASE_URL")
            .orElse("https://aster-app-jsng.onrender.com")
            .get()
            .trimEnd('/')
        manifestPlaceholders["apiBaseUrl"] = apiBaseUrl
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release.jks")
            storePassword = "aster2026"
            keyAlias = "aster_release_key"
            keyPassword = "aster2026"
        }
    }

    buildTypes {
        getByName("debug") {
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.halilibo.compose-richtext:richtext-ui-material3:0.17.0")
    implementation("com.halilibo.compose-richtext:richtext-commonmark:0.17.0")
}
