plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.room)
}

android {
    namespace = "com.flixclusive.core.database"

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    implementation(libs.stubs.util)
    implementation(libs.stubs.model.film)

    implementation(libs.gson)
}
