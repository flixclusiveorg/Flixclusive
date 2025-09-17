plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.feature.mobile.library.manage"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatabase)
    implementation(projects.coreDrawables)
    implementation(projects.coreNavigation)
    implementation(projects.coreNetwork)
    implementation(projects.corePresentationCommon)
    implementation(projects.coreStrings)
    implementation(projects.dataDatabase)
    implementation(projects.feature.mobile.libraryCommon)

    implementation(libs.coil.compose)
    implementation(libs.compose.adaptive.layout)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.immutables)
    implementation(libs.lifecycle.runtimeCompose)
    implementation(libs.stubs.model.film)

    testImplementation(projects.coreTesting)
}
