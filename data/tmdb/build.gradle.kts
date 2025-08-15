plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.tmdb"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreNetwork)
    implementation(projects.coreStrings)
    implementation(libs.stubs.util)
    implementation(libs.stubs.model.film)
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.provider)
    implementation(libs.jsoup)

    testImplementation(projects.coreTesting)
}
