@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.feature)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.mobile.genre"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.ui.mobile)
    implementation(projects.data.tmdb)

    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
}