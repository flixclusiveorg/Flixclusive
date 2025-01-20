plugins {
    alias(libs.plugins.flixclusive.feature)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.feature.mobile.user"
}

dependencies {
    implementation(projects.core.ui.mobile)
    implementation(projects.data.configuration)
    implementation(projects.data.search)
    implementation(projects.data.libraryRecent)
    implementation(projects.data.libraryWatchlist)
    implementation(projects.domain.user)
    implementation(projects.domain.provider)

    implementation(libs.compose.adaptive)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.palette)
}
