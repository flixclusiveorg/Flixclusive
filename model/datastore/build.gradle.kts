@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
}

android {
    namespace = "com.flixclusive.model.datastore"
}

dependencies {
    api(libs.gson)
    api(libs.stubs.provider)
    implementation(projects.core.locale)
}