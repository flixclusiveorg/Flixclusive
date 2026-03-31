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
    implementation(libs.stubs.model.film)
    implementation(projects.coreCommon)
    implementation(projects.coreDatastore)
    implementation(projects.coreDatabase)

    testImplementation(libs.stubs.model.provider)
    testImplementation(projects.coreTesting)
}
