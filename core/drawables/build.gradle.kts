plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
    `maven-publish`
}

android {
    namespace = "com.flixclusive.core.drawables"
}

dependencies {
    // ...
}
