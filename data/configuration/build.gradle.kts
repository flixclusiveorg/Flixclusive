plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.data.configuration"
}

dependencies {
    api(libs.stubs.util)
    api(projects.core.datastore)
    api(projects.model.configuration)

    implementation(libs.mockk)
    implementation(projects.core.locale)
    implementation(projects.core.network)


    testImplementation(libs.retrofit.gson)
}
