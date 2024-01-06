plugins {
    alias(libs.plugins.flixclusive.library)
}

android {
    namespace = "com.flixclusive.provider.base"
}

dependencies {
    api(projects.extractor.base)
    api(projects.core.util)
    api(projects.model.provider)
    api(libs.okhttp)
    api(libs.jsoup)
    api(libs.gson)

    implementation(libs.mockk)
    implementation(libs.coroutines.test)
    implementation(libs.junit)

    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
}