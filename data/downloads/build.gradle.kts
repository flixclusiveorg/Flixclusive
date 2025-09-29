plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.app.updates"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreNetwork)
    implementation(projects.coreDatastore)

    implementation(libs.okhttp)
    implementation(libs.stubs.util)

    testImplementation(projects.coreTesting)
    testImplementation(libs.retrofit.gson)
    testImplementation(libs.okhttp.mockwebserver)
}
