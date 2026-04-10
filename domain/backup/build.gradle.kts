plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.backup"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.dataBackup)

    implementation(libs.kotlinx.serialization.protobuf)
}
