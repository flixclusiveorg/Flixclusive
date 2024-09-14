@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.tmdb"
}

dependencies {
    api(projects.core.util)
    api(projects.core.locale)
    api(projects.model.film)

    implementation(libs.coroutines.test)
    implementation(libs.mockk)
    implementation(projects.core.network)
    implementation(projects.data.configuration)
}