plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
}

android {
    namespace = "com.flixclusive.core.ui.mobile"
}

dependencies {
    api(projects.core.ui.common)

    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.model.film)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.core.ktx)
    implementation(libs.destinations.animations)
}