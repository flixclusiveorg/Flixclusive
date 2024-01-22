plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.extractor.base"
}

dependencies {
    api(libs.jsoup)
    api(libs.okhttp)
    api(projects.core.util)
    api(projects.model.provider)
}