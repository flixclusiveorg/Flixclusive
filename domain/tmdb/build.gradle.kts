plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.domain.tmdb"
}

dependencies {
    implementation(projects.coreNetwork)
    implementation(projects.coreCommon)
    implementation(projects.dataTmdb)
    implementation(projects.dataProvider)
    implementation(libs.stubs.model.film)
    implementation(libs.stubs.util)
}
