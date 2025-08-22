plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.domain.provider"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatastore)
    implementation(projects.coreDatabase)
    implementation(projects.coreDrawables)
    implementation(projects.coreStrings)
    implementation(projects.coreNetwork)
    implementation(projects.dataProvider)
    implementation(projects.dataTmdb)
    implementation(libs.pauseCoroutineDispatcher)
    implementation(libs.stubs.model.film)
    implementation(libs.stubs.provider)
    implementation(libs.stubs.util)

    androidTestImplementation(projects.coreTesting)
    testImplementation(projects.coreTesting)
}
