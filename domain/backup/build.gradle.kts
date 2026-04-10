plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.domain.backup"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatastore)
    implementation(projects.dataBackup)

    implementation(libs.work.runtime.ktx)

    implementation(libs.kotlinx.serialization.protobuf)
}
