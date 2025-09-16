plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.core.common"
}

dependencies {
    implementation(libs.stubs.util)
    implementation(libs.stubs.model.film)
    implementation(libs.stubs.model.provider)
    implementation(libs.okhttp)

    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
}
