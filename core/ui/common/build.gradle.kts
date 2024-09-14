@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
}

android {
    namespace = "com.flixclusive.core.ui.common"
}

dependencies {
    api(projects.core.theme)
    api(libs.stubs.util)
    api(projects.core.locale)
    implementation(projects.model.database)
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.model.film)
    implementation(projects.model.datastore)

    implementation(libs.coil.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
}