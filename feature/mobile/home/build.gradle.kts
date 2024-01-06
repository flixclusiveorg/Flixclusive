@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.feature)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.mobile.home"
}

dependencies {
}