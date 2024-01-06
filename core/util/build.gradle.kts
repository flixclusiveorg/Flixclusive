plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.core.util"
}

dependencies {
    api(libs.okhttp)

    implementation(projects.model.database)
    implementation(projects.model.tmdb)
    implementation(libs.retrofit)
    implementation(libs.compose.ui)
    implementation(libs.compose.runtime)
}