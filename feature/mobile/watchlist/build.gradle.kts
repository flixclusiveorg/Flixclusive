plugins {
    alias(libs.plugins.flixclusive.feature)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.mobile.watchlist"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.ui.mobile)
    implementation(projects.data.libraryWatchlist)
    implementation(projects.domain.user)

    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.lifecycle.runtimeCompose)
}
