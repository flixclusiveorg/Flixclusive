plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.data.user"
}

dependencies {
    api(libs.stubs.util)
    api(projects.model.database)

    implementation(projects.core.database)
}