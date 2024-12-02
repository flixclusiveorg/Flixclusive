@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.domain.user"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.data.user)
}