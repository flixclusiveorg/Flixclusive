plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.feature.app.updates"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.dataAppUpdates)
    implementation(projects.dataDownloads)
    implementation(projects.domainDownloads)

    implementation(libs.compose.runtime)

    testImplementation(projects.coreTesting)
}
