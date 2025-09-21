plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.feature.mobile.provider.manage"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatastore)
    implementation(projects.coreDrawables)
    implementation(projects.coreNavigation)
    implementation(projects.corePresentationCommon)
    implementation(projects.corePresentationMobile)
    implementation(projects.coreStrings)
    implementation(projects.dataProvider)
    implementation(projects.domainProvider)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtimeCompose)
    implementation(libs.stubs.provider)
    implementation(libs.stubs.model.provider)

    testImplementation(projects.coreTesting)
}
