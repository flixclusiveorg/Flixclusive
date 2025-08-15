plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.domain.catalog"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreNetwork)
    implementation(projects.coreDatabase)
    implementation(projects.dataDatabase)
    implementation(projects.dataProvider)
    implementation(projects.dataTmdb)

    implementation(libs.stubs.model.film)
    implementation(libs.stubs.provider)
    implementation(libs.stubs.util)

    testImplementation(projects.coreTesting)
}
