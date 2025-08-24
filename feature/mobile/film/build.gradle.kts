plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.mobile.film"
}

dependencies {
    // implementation(projects.core.ui.mobile)
    // implementation(projects.core.ui.film)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.animation)
    implementation(libs.compose.animation.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtimeCompose)
}
