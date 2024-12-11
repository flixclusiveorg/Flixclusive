plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.domain.tmdb"
}

dependencies {
    api(projects.data.tmdb)
    api(projects.data.watchHistory)
    api(projects.data.provider)
}