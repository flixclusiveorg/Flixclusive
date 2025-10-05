plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.core.presentation.player"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDrawables)
    implementation(projects.coreStrings)
    implementation(projects.coreDatastore)

    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.viewbinding)
    implementation(libs.lifecycle.viewModelKtx)
    implementation(libs.media3.ffmpeg)
    implementation(libs.universalchardet)
    implementation(libs.stubs.util)
    // implementation(libs.media3.cast)

    api(libs.media3.common)
    api(libs.media3.common.ktx)
    api(libs.media3.datasource.okhttp)
    api(libs.media3.exoplayer)
    api(libs.media3.exoplayer.hls)
    api(libs.media3.exoplayer.dash)
    api(libs.media3.session)
    api(libs.media3.ui)
    implementation(libs.media3.ui.compose)

    androidTestImplementation(projects.coreTesting)
    androidTestImplementation(libs.media3.test.utils)
}
