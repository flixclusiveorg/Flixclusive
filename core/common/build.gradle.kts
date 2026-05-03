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
    implementation(libs.stubs.model.media)
    implementation(libs.stubs.model.provider)
    implementation(libs.okhttp)
    implementation(libs.unifile)

    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.animation)
}
