plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.compose)
    `maven-publish`
}

android {
    namespace = "com.flixclusive.core.locale"
}

dependencies {
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
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
            artifactId = "core-locale"
            version = "1.0.0"
            artifact(sourcesJar)
            artifact("build/outputs/aar/locale-release.aar")
        }
    }
}