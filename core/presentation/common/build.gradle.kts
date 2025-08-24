plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
}

android {
    namespace = "com.flixclusive.core.presentation.common"
}

dependencies {
    implementation(projects.coreStrings)

    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.kotlinx.immutables)
    implementation(libs.material)
}
