// Top-level build file. Plugins are declared here and applied in :app.
// AGP 9 has built-in Kotlin support, so the org.jetbrains.kotlin.android plugin is NOT used.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
}
