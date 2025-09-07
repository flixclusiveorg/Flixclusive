plugins {
    alias(libs.plugins.flixclusive.library)
}

android {
    namespace = "com.flixclusive.core.navigation"
}

dependencies {
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.model.film)
}
