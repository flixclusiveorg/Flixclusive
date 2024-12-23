plugins {
    alias(libs.plugins.flixclusive.feature)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.splashScreen"
}

dependencies {
    implementation(projects.core.ui.common)
    implementation(projects.core.ui.mobile)
    implementation(projects.domain.updater)
    implementation(projects.domain.user)
    implementation(projects.domain.home)

    implementation(libs.accompanist.permissions)
    implementation(libs.compose.animation)
    implementation(libs.compose.animation.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtimeCompose)
}