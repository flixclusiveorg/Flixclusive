plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.core.ui.film"
}

dependencies {
    api(projects.core.datastore)
    api(projects.core.ui.common)
    api(projects.core.ui.tv)
    api(projects.data.watchHistory)
    api(projects.domain.database)
    api(projects.domain.tmdb)
    api(projects.domain.user)

    api(libs.lifecycle.viewModelCompose)
}