plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.domain.library.watchlist"
}

dependencies {
    api(projects.data.libraryRecent)
    api(projects.data.libraryWatchlist)
    api(libs.stubs.model.film)

    implementation(libs.stubs.util)
}
