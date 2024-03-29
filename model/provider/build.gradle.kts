@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
}

android {
    namespace = "com.flixclusive.model.provider"
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(6, TimeUnit.HOURS)
}


dependencies {
    api(libs.compose.runtime)
    api(libs.gson)
    api(libs.flixclusive.gradle) {
        isChanging = true
    }

    implementation(projects.core.util)
}