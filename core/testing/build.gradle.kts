plugins {
    alias(libs.plugins.flixclusive.library)
}

android {
    namespace = "com.flixclusive.core.testing"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatabase)
    implementation(projects.coreNetwork)

    implementation(libs.coroutines.test)
    implementation(libs.junit)
    implementation(libs.mockk)
    implementation(libs.strikt)
    implementation(libs.turbine)
    implementation(libs.androidx.test.ext.junit)

    implementation(libs.retrofit.gson)
    implementation(libs.stubs.util)
    implementation(libs.stubs.model.film)

    implementation(libs.room.runtime)
    implementation(libs.room.testing)
    implementation(libs.room.ktx)
}
