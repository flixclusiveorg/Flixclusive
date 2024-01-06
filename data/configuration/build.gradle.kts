@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.configuration"
}

dependencies {
    api(projects.core.datastore)
    api(projects.model.configuration)
    api(projects.core.util)

    implementation(projects.core.network)
    implementation(projects.data.provider)
}