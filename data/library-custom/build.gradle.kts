plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.data.library.custom"
}

dependencies {
    api(libs.stubs.util)
    api(projects.core.locale)
    api(projects.model.database)

    testImplementation(libs.stubs.model.provider)
    implementation(projects.core.database)
}
