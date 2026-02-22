plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.core.navigation"
}

ksp {

}

dependencies {
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.model.film)
}
