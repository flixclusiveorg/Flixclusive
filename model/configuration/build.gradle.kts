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