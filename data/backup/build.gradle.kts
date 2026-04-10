plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.data.backup"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatabase)
    implementation(projects.coreDatastore)

    implementation(libs.stubs.util)
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.model.film)

    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.unifile)

    androidTestImplementation(projects.coreTesting)
    androidTestImplementation(libs.room.testing)
}
