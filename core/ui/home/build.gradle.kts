plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.core.ui.home"
}

dependencies {
    api(projects.core.datastore)
//     api(projects.domain)
    // implementation(projects.core.ui.tv)
//    implementation(projects.data)
//     implementation(projects.domain)

    implementation(libs.compose.runtime)
    implementation(libs.lifecycle.viewModelCompose)
}
