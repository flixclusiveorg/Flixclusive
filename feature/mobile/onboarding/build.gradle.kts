plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.feature.mobile.onboarding"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatastore)
    implementation(projects.coreDrawables)
    implementation(projects.coreStrings)
    implementation(projects.coreNavigation)
    implementation(projects.corePresentationCommon)
    implementation(projects.corePresentationMobile)

    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtimeCompose)

    implementation(libs.unifile)

    testImplementation(projects.coreTesting)
}
