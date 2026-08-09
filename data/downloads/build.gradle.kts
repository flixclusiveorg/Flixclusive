plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.downloads"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatabase)
    implementation(projects.coreNetwork)
    implementation(projects.coreDatastore)
    implementation(projects.coreStrings)

    implementation(libs.okhttp)
    implementation(libs.stubs.util)
    implementation(libs.stubs.model.media)
    implementation(libs.unifile)
    implementation(libs.media3.exoplayer.hls)

    testImplementation(projects.coreTesting)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(projects.coreTesting)
    androidTestImplementation(libs.okhttp.mockwebserver)
}
