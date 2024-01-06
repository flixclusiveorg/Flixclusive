plugins {
    alias(libs.plugins.flixclusive.library)
}

android {
    namespace = "com.flixclusive.extractor.base"
}

dependencies {
    api(libs.okhttp)
    api(projects.model.provider)
    api(projects.core.util)
}