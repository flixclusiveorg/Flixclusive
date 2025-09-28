plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.testing)
}

android {
    // TODO: Update this when we remove `search` module
    namespace = "com.flixclusive.feature.mobile.searchExpanded"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatastore)
    implementation(projects.coreDrawables)
    implementation(projects.coreDatabase)
    implementation(projects.coreNavigation)
    implementation(projects.coreNetwork)
    implementation(projects.corePresentationCommon)
    implementation(projects.corePresentationMobile)
    implementation(projects.coreStrings)
    implementation(projects.dataDatabase)
    implementation(projects.dataProvider)
    implementation(projects.dataTmdb)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.immutables)
    implementation(libs.lifecycle.runtimeCompose)
    implementation(libs.stubs.model.film)
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.provider)

    implementation(projects.coreTesting)
}
