plugins {
    alias(libs.plugins.flixclusive.feature.tv)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.tv.film"
}

dependencies {
    // implementation(projects.core.ui.film)
    // implementation(projects.core.ui.player)
    // implementation(projects.core.ui.tv)
    implementation(libs.stubs.model.film)
    implementation(projects.feature.tv.player)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.tv.foundation)
    implementation(libs.compose.tv.material)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtimeCompose)
}
