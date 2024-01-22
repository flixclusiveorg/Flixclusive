

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
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
    api(projects.domain.database)
    api(projects.domain.provider)

    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.viewbinding)
    implementation(libs.lifecycle.viewModelKtx)
    // implementation(libs.media3.cast)
    api(libs.media3.common)
    api(libs.media3.datasource.okhttp)
    api(libs.media3.exoplayer)
    api(libs.media3.exoplayer.hls)
    api(libs.media3.session)
    api(libs.media3.ui)
}