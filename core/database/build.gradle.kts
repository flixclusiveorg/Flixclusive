plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.room)
}

android {
    namespace = "com.flixclusive.core.database"
}

dependencies {
    implementation(libs.stubs.util)
    implementation(projects.model.database)
    implementation(libs.stubs.model.film)

    implementation(libs.gson)
}