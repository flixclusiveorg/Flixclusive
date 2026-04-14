plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.data.backup"

    defaultConfig {
        testInstrumentationRunner = "com.flixclusive.data.backup.HiltTestRunner"
    }
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatabase)
    implementation(projects.coreDatastore)

    implementation(libs.work.runtime.ktx)

    implementation(libs.stubs.util)
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.model.film)

    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.unifile)

    kspAndroidTest(libs.hilt.compiler)

    androidTestImplementation(projects.coreTesting)
    androidTestImplementation(libs.hilt.testing)
    androidTestImplementation(libs.work.testing)
    androidTestImplementation(libs.room.testing)
}
