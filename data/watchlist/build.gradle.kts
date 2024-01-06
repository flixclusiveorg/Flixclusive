@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.watchlist"
}

dependencies {
    api(projects.model.database)
    api(projects.core.util)

    implementation(projects.core.database)
}