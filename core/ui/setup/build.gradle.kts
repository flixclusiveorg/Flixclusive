@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
}

android {
    namespace = "com.flixclusive.core.ui.setup"
}

dependencies {
    api(projects.data.configuration)

    implementation(libs.lifecycle.viewModelKtx)
}