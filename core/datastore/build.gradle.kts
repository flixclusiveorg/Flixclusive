@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.core.datastore"
}

dependencies {
    api(libs.dataStore.preferences)
    api(projects.model.datastore)

    implementation(libs.stubs.util)
}