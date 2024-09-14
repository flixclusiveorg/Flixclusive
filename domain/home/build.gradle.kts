@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.domain.home"
}

dependencies {
    api(projects.data.configuration)
    api(projects.data.watchHistory)
    api(projects.domain.catalog)
    api(projects.domain.provider)
    api(projects.domain.tmdb)

    implementation(projects.core.network)
}