plugins {
    alias(libs.plugins.flixclusive.library)
}

android {
    namespace = "com.flixclusive.provider.superstream"
}

dependencies {
    implementation(projects.provider.base)

    // TODO: Conventional Testing
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}