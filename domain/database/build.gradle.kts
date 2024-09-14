@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.domain.database"
}

dependencies {
    api(projects.data.watchHistory)
    api(projects.data.watchlist)
    api(libs.stubs.model.film)

    implementation(libs.stubs.util)
}