

plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.core.ui.player"
}

dependencies {
    api(projects.core.ui.common)
    api(projects.core.datastore)
    api(projects.domain.tmdb)
    api(projects.domain.libraryRecent)
    api(projects.domain.provider)
    api(projects.domain.user)

    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.viewbinding)
    implementation(libs.lifecycle.viewModelKtx)
    implementation(libs.media3.ffmpeg)
    // implementation(libs.media3.cast)

    api(libs.media3.common)
    api(libs.media3.datasource.okhttp)
    api(libs.media3.exoplayer)
    api(libs.media3.exoplayer.hls)
    api(libs.media3.session)
    api(libs.media3.ui)
}
