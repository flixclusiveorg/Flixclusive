plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
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
