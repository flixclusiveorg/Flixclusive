plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.data.search"
}

dependencies {
    api(libs.stubs.util)
    api(projects.model.database)

    implementation(projects.core.database)
}
