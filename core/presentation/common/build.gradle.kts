plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.core.presentation.common"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDrawables)
    implementation(projects.coreStrings)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.activity)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.hilt.navigation)
    implementation(libs.kotlinx.immutables)
    implementation(libs.material)
    implementation(libs.stubs.model.media)
    implementation(libs.stubs.model.provider)
}
