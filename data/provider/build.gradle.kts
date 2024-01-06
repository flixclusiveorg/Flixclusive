@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.provider"
}

dependencies {
    api(projects.core.datastore)
    api(projects.model.datastore)
    api(projects.model.provider)
    api(projects.core.util)
    api(libs.compose.runtime)

    implementation(projects.data.tmdb)
    implementation(libs.okhttp)
}