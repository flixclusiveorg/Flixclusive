plugins {
    alias(libs.plugins.flixclusive.library)
}

android {
    namespace = "com.flixclusive.extractor.upcloud"
}

dependencies {
    implementation(projects.extractor.base)

    implementation(libs.jsoup)
}