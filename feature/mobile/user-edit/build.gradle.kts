plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.feature.mobile.user"
}

dependencies {
    implementation(projects.coreDatastore)
    implementation(projects.coreDatabase)
    implementation(projects.coreCommon)
    implementation(projects.corePresentationMobile)
    implementation(projects.corePresentationCommon)
    implementation(projects.coreDrawables)
    implementation(projects.coreNavigation)
    implementation(projects.coreNetwork)
    implementation(projects.coreStrings)
    implementation(projects.dataDatabase)
    implementation(projects.dataProvider)
    implementation(projects.domainProvider)

    implementation(libs.compose.adaptive)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.palette)
    implementation(libs.stubs.model.provider)

    testImplementation(projects.coreTesting)
}
