plugins {
    alias(libs.plugins.flixclusive.library)
}

android {
    namespace = "com.flixclusive.extractor.mixdrop"
}

dependencies {
    implementation(projects.extractor.base)

    implementation(libs.jsoup)
}