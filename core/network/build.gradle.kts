plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.core.network"
}

dependencies {
    api(libs.okhttp)
    api(libs.okhttp.dnsoverhttps)
    api(libs.retrofit)

    implementation(libs.conscrypt)
    implementation(libs.mockk)
    implementation(libs.retrofit.gson)
    implementation(projects.coreCommon)
    implementation(projects.coreDatastore)
    implementation(projects.coreStrings)
    implementation(libs.stubs.util)
    implementation(libs.stubs.model.film)
}
