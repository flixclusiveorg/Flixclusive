@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.domain.provider"
}

dependencies {
    api(projects.data.provider)
    api(projects.data.tmdb)
    api(projects.model.database)

    implementation(projects.core.network)
    implementation(libs.compose.ui.util)
    implementation(libs.mockk)
    implementation(libs.pauseCoroutineDispatcher)
}