plugins {
    alias(libs.plugins.flixclusive.feature.mobile)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = "com.flixclusive.feature.mobile.player"
}

dependencies {
    implementation(projects.coreCommon)
    implementation(projects.coreDatabase)
    implementation(projects.coreDatastore)
    implementation(projects.coreDrawables)
    implementation(projects.coreNavigation)
    implementation(projects.coreNetwork)
    implementation(projects.corePresentationCommon)
    implementation(projects.corePresentationMobile)
    implementation(projects.corePresentationPlayer)
    implementation(projects.coreStrings)
    implementation(projects.dataDatabase)
    implementation(projects.dataProvider)
    implementation(projects.domainDatabase)
    implementation(projects.domainProvider)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.util)
    implementation(libs.lifecycle.runtimeCompose)
    implementation(libs.media3.cast)
    implementation(libs.media3.common)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.stubs.model.film)
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.provider)
    implementation(libs.stubs.util)
    implementation(libs.unifile)

    testImplementation(projects.coreTesting)
}
