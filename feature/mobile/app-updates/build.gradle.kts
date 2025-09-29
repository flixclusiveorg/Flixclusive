plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.mobile.app.updates"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDrawables)
    implementation(projects.coreNavigation)
    implementation(projects.coreStrings)
    implementation(projects.corePresentationCommon)
    implementation(projects.dataAppUpdates)
    implementation(projects.dataDownloads)
    implementation(projects.domainDownloads)
    implementation(projects.feature.appUpdates)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.markdown)
}
