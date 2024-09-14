@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.feature)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.tv.film"
}

dependencies {
    implementation(projects.core.ui.film)
    implementation(projects.core.ui.player)
    implementation(projects.core.ui.tv)
    implementation(projects.model.database)
    implementation(libs.stubs.model.film)
    implementation(projects.feature.tv.player)

    implementation(libs.coil.compose)
    implementation(libs.compose.tv.foundation)
    implementation(libs.compose.tv.material)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtimeCompose)
}