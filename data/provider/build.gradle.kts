plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.provider"
}

dependencies {
    implementation(libs.stubs.util)
    implementation(libs.stubs.provider)
    implementation(libs.stubs.model.media)
    implementation(libs.okhttp)
    implementation(projects.coreCommon)
    implementation(projects.coreDatastore)
    implementation(projects.coreDatabase)
    implementation(libs.work.runtime.ktx)

    testImplementation(libs.stubs.model.provider)
    testImplementation(projects.coreTesting)
}
