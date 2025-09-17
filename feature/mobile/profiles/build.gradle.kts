plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.mobile.profiles"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatabase)
    implementation(projects.coreDatastore)
    implementation(projects.coreDrawables)
    implementation(projects.coreNavigation)
    implementation(projects.corePresentationCommon)
    implementation(projects.corePresentationMobile)
    implementation(projects.coreStrings)
    implementation(projects.dataDatabase)
    implementation(projects.dataProvider)
    implementation(projects.domainProvider)

    implementation(libs.stubs.util)
    implementation(libs.stubs.model.provider)
    implementation(libs.coil.compose)
    implementation(libs.compose.adaptive)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.palette)

    testImplementation(projects.coreTesting)
}
