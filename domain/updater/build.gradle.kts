plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.domain.updater"
}

dependencies {
    api(projects.data.configuration)
    implementation(projects.data.provider)
}