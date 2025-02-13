plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.data.provider"
}

dependencies {
    api(libs.compose.runtime)
    api(projects.core.datastore)
    api(projects.core.network)
    api(projects.core.ui.common)
    api(libs.stubs.util)
    api(projects.model.datastore)
    api(libs.stubs.model.provider)
    api(libs.stubs.model.film)
    api(libs.stubs.provider)

    implementation(libs.okhttp)
    implementation(libs.mockk)
}
