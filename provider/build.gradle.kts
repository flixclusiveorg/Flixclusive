plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.provider"
}

dependencies {
    api(libs.gson)
    api(libs.jsoup)
    api(libs.okhttp)
    api(libs.flixclusive.gradle)
    api(projects.core.util)
    api(projects.model.provider)

    implementation(libs.coroutines.test)
    implementation(libs.junit)
    implementation(libs.mockk)
}