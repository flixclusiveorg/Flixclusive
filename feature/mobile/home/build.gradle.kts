@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.feature)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.mobile.home"
}

dependencies {
    implementation(projects.core.ui.mobile)
    implementation(projects.core.ui.home)
    implementation(projects.model.configuration)
    implementation(projects.model.database)
    implementation(libs.stubs.model.film)

    implementation(libs.coil.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.lifecycle.runtimeCompose)
}