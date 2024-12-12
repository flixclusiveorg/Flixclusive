plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.domain.user"
}

dependencies {
    api(projects.data.user)

    implementation(projects.core.datastore)
}