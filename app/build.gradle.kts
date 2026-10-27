plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val apiUrlDebug = providers.gradleProperty("AGHOUR_API_URL_DEBUG").getOrElse("http://10.0.2.2:3000/api/v1/")
val apiUrlRelease = providers.gradleProperty("AGHOUR_API_URL_RELEASE").getOrElse("https://api.example.com/api/v1/")

android {
    namespace = "com.aghour.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aghour.app"
        minSdk = 26
        targetSdk = 36 // Google Play requires API 36+ for new apps (since Aug 31, 2026)
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$apiUrlDebug\"")
        }
        release {
            isMinifyEnabled = false // enable R8 + rules before the first Play release
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE_URL", "\"$apiUrlRelease\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    // Category / menu icons (Mosque, LocalPharmacy, Storefront…). Large: R8 strips unused icons in release.
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
}
