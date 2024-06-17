plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.compose)
}

android {
    namespace = "com.flixclusive.provider"
}
dependencies {
    api(libs.gson)
    api(libs.jsoup)
    api(libs.okhttp)
    api(projects.core.util)
    api(projects.model.provider)
    api(projects.model.tmdb)

    implementation(libs.coil.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.tv.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.constraintlayout.compose)
    implementation(libs.coroutines.test)
    implementation(libs.junit)
    implementation(libs.lifecycle.runtimeCompose)
}