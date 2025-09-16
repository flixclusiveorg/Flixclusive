plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.feature.mobile.library.details"
}

dependencies {
    implementation(projects.feature.mobile.libraryCommon)
    implementation(projects.coreNetwork)
    implementation(projects.coreCommon)
    implementation(projects.coreDatabase)
    implementation(projects.coreDrawables)
    implementation(projects.coreNavigation)
    implementation(projects.corePresentationCommon)
    implementation(projects.coreStrings)
    implementation(projects.dataDatabase)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.stubs.model.film)
    implementation(libs.compose.adaptive.layout)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.immutables)
    implementation(libs.lifecycle.runtimeCompose)

    testImplementation(projects.coreTesting)
}
