plugins {
    alias(libs.plugins.flixclusive.feature.tv)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.tv.preferences"
}

dependencies {
    implementation(libs.compose.tv.foundation)
    implementation(libs.compose.tv.material)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.lifecycle.runtimeCompose)
}
