plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.feature.mobile.provider.test"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.corePresentationCommon)
    implementation(projects.corePresentationMobile)
    implementation(projects.coreNavigation)
    implementation(projects.coreStrings)
    implementation(projects.coreDrawables)
    implementation(projects.dataProvider)
    implementation(projects.domainProvider)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.adaptive)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtimeCompose)
    implementation(libs.stubs.provider)
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.model.film)

    testImplementation(projects.coreTesting)
}
