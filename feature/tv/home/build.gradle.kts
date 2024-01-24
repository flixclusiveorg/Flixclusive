@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.feature)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.tv.home"
}

dependencies {
    implementation(projects.core.ui.common)
    implementation(projects.core.ui.home)
    implementation(projects.core.ui.tv)
    implementation(projects.domain.home)

    implementation(libs.coil.compose)
    implementation(libs.compose.tv.foundation)
    implementation(libs.compose.tv.material)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui.alpha)
    implementation(libs.lifecycle.runtimeCompose)
}