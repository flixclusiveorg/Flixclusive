@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.room)
}

android {
    namespace = "com.flixclusive.core.database"
}

dependencies {
    implementation(projects.model.database)
    implementation(projects.model.tmdb)
    implementation(projects.core.util)

    implementation(libs.gson)
}