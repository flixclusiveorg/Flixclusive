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
    implementation(projects.core.datastore)
    implementation(projects.core.locale)
    implementation(libs.stubs.util)
    implementation(projects.model.configuration)
    implementation(projects.model.datastore)
    implementation(libs.stubs.model.film)
}