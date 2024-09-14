plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.service"
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.okhttp)
    implementation(projects.core.ui.common)
    implementation(libs.stubs.util)
    implementation(projects.data.configuration)
}