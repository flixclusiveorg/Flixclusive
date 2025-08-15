plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.core.ui.film"
}

dependencies {
    api(projects.core.datastore)
    api(projects.core.ui.common)
    api(projects.core.ui.tv)
//    api(projects.data)
//     api(projects.domain)
//     api(projects.domain)
//     api(projects.domain)

    api(libs.lifecycle.viewModelCompose)
}
