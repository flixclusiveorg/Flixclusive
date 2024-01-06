@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.tmdb"
}

dependencies {
    api(projects.data.configuration)
    api(projects.model.tmdb)
    api(projects.core.util)

    implementation(projects.core.network)
}