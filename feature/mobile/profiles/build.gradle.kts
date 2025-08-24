plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.mobile.profiles"
}

dependencies {
    // implementation(projects.core.ui.mobile)
////    implementation(projects.data)
//     implementation(projects.domain)
//     implementation(projects.domain)
//     implementation(projects.domain)

    implementation(libs.compose.adaptive)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.palette)
}
