@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
}

android {
    namespace = "com.flixclusive.core.ui.tv"
}

dependencies {
    api(projects.core.ui.common)
    implementation(libs.stubs.model.film)
    implementation(libs.stubs.model.provider)
    implementation(projects.model.database)

    implementation(libs.coil.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.tv.foundation)
    implementation(libs.compose.tv.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.destinations.animations)
}