plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.compose)
}

android {
    namespace = "com.flixclusive.core.util"
}

dependencies {
    api(libs.okhttp)

    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.gson)
    implementation(libs.jsoup)
    implementation(libs.junit)
    implementation(libs.mockk)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.retrofit)
}