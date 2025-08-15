plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.database"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatabase)
    implementation(projects.coreDatastore)
    implementation(libs.stubs.model.film)
    implementation(libs.stubs.util)

    androidTestImplementation(libs.room.runtime)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.room.ktx)
    androidTestImplementation(projects.coreTesting)
}
