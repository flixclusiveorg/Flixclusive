@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.core.ui"
}

dependencies {
    api(projects.core.util)
    api(projects.model.tmdb)
    api(projects.core.theme)

    implementation(libs.compose.ui)
    implementation(libs.compose.runtime)
    implementation(libs.lifecycle.viewModelKtx)
}