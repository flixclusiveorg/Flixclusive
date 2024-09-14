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
    api(libs.stubs.util)
    api(projects.core.locale)
    api(projects.core.network)
    api(libs.stubs.model.film)

    implementation(libs.coroutines.test)
    implementation(libs.mockk)
    implementation(projects.data.configuration)
}