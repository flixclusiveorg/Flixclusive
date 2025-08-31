plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
}

android {
    namespace = "com.flixclusive.core.presentation.mobile"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDrawables)
    implementation(projects.coreStrings)
    implementation(projects.corePresentationCommon)

    implementation(libs.compose.adaptive)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coil.compose)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.kotlinx.immutables)
    implementation(libs.material)
    implementation(libs.palette)
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.model.film)
}
