plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.room)
}

android {
    namespace = "com.flixclusive.model.database"
}

dependencies {
    api(libs.gson)
    api(libs.stubs.util)
    api(libs.stubs.model.film)
}