plugins {
    alias(libs.plugins.flixclusive.feature)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.tv.player"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.ui.common)
    implementation(projects.core.ui.player)
    implementation(projects.core.ui.tv)
    implementation(projects.domain.libraryRecent)
    implementation(projects.domain.provider)
    implementation(projects.domain.tmdb)

    implementation(libs.compose.tv.foundation)
    implementation(libs.compose.tv.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtimeCompose)
}
