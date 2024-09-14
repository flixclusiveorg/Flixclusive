plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    alias(libs.plugins.flixclusive.compose)
    `maven-publish`
}

android {
    namespace = "com.flixclusive.provider"
}

dependencies {
    api(libs.jsoup)
    api(libs.okhttp)
    api(libs.stubs.util)
    api(libs.stubs.model.provider)
    api(libs.stubs.model.film)

    implementation(libs.compose.runtime)
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from("src/main/kotlin")
}

publishing {
    repositories {
        mavenLocal()
    }

    publications {
        create<MavenPublication>("release") {
            groupId = "com.flixclusive"
            artifactId = "provider"
            version = "1.0.0"
            artifact(sourcesJar)
            artifact("build/outputs/aar/provider-release.aar")
        }
    }
}