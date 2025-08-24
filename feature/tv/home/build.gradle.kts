plugins {
    alias(libs.plugins.flixclusive.feature.tv)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.tv.home"
}

dependencies {
    // implementation(projects.core.ui.common)
    // implementation(projects.core.ui.home)
    // implementation(projects.core.ui.tv)
//     implementation(projects.domain)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.tv.foundation)
    implementation(libs.compose.tv.material)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui.alpha)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtimeCompose)
    implementation(libs.palette)
}
