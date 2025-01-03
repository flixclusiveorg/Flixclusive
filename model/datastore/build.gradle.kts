plugins {
    alias(libs.plugins.flixclusive.library)
}

android {
    namespace = "com.flixclusive.model.datastore"
}

dependencies {
    api(libs.gson)
    api(libs.stubs.provider)
    api(libs.dataStore.preferences)
    implementation(projects.core.locale)
}