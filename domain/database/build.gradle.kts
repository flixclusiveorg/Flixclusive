plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.domain.database"
}

dependencies {
    implementation(projects.coreDatabase)
    implementation(projects.dataDatabase)
    implementation(libs.stubs.model.film)
}
