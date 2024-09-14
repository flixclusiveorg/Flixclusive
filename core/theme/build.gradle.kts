@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
}

android {
    namespace = "com.flixclusive.core.theme"
}

dependencies {
    implementation(libs.compose.material3)
    implementation(libs.compose.tv.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.material)
}