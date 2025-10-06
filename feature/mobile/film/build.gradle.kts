plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.feature.mobile.film"
}

dependencies {
    // implementation(projects.core.ui.mobile)
    // implementation(projects.core.ui.film)
    implementation(projects.coreCommon)
    implementation(projects.coreStrings)
    implementation(projects.coreDrawables)
    implementation(projects.coreNavigation)
    implementation(projects.coreNetwork)
    implementation(projects.coreDatabase)
    implementation(projects.coreDatastore)
    implementation(projects.corePresentationCommon)
    implementation(projects.corePresentationMobile)
    implementation(projects.dataDatabase)
    implementation(projects.domainDatabase)
    implementation(projects.dataProvider)
    implementation(projects.dataTmdb)
    implementation(projects.domainProvider)
    implementation(projects.feature.mobile.libraryCommon)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.adaptive)
    implementation(libs.compose.animation)
    implementation(libs.compose.animation.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.immutables)
    implementation(libs.lifecycle.runtimeCompose)
    implementation(libs.stubs.model.film)
    implementation(libs.stubs.model.provider)

    testImplementation(projects.coreTesting)
}
