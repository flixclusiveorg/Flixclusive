plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.mobile.genre"
}

dependencies {
    implementation(projects.coreDatastore)
    // implementation(projects.core.ui.mobile)
//    implementation(projects.data)

    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
}
