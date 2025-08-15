plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.core.datastore"
}

dependencies {
    api(libs.dataStore.preferences)

    implementation(projects.coreCommon)
    implementation(libs.stubs.util)
    implementation(libs.stubs.model.provider)
}
