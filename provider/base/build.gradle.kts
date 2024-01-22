plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.provider.base"
}

dependencies {
    api(libs.gson)
    api(libs.jsoup)
    api(libs.okhttp)
    api(projects.core.util)
    api(projects.extractor.base)

    implementation(projects.model.provider)
    implementation(libs.coroutines.test)
    implementation(libs.junit)
    implementation(libs.mockk)
}