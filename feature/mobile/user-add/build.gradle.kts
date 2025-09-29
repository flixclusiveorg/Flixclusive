plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.feature.mobile.user.add"
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
    implementation(projects.dataTmdb)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.adaptive)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.immutables)
    implementation(libs.palette)
    implementation(libs.stubs.model.film)

    testImplementation(projects.coreTesting)
}
