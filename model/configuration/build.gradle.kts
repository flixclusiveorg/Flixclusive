@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
}

android {
    namespace = "com.flixclusive.model.configuration"
}

dependencies {
    api(libs.gson)
    implementation(libs.stubs.model.provider)
}