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
    api(libs.compose.runtime)
    api(projects.core.datastore)
    api(projects.core.ui.common)
    api(projects.core.util)
    api(projects.model.datastore)
    api(projects.model.provider)
    api(projects.model.tmdb)
    api(projects.provider.base)

    implementation(libs.okhttp)
    implementation(projects.provider.flixhq)
    implementation(projects.provider.lookmovie)
    implementation(projects.provider.superstream)
}