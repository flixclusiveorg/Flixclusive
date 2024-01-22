plugins {
    alias(libs.plugins.flixclusive.provider)
}

android {
    namespace = "com.flixclusive.provider.flixhq"
}

dependencies {
    implementation(projects.extractor.upcloud)
}