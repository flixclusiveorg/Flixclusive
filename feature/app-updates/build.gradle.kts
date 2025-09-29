plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.feature.app.updates"
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.okhttp)
    implementation(libs.stubs.util)
}
