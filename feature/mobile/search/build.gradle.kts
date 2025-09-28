plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
}

android {
    namespace = "com.flixclusive.feature.mobile.search"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDrawables)
    implementation(projects.coreNavigation)
    implementation(projects.coreNetwork)
    implementation(projects.corePresentationCommon)
    implementation(projects.corePresentationMobile)
    implementation(projects.coreStrings)
    implementation(projects.dataProvider)
    implementation(projects.dataTmdb)
    implementation(projects.domainCatalog)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.adaptive)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtimeCompose)
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.provider)
}
