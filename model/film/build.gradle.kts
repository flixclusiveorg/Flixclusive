@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.library)
    `maven-publish`
}

android {
    namespace = "com.flixclusive.model.film"
}

dependencies {
    api(libs.gson)
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
            artifactId = "model-film"
            version = "1.0.0"
            artifact(sourcesJar)
            artifact("build/outputs/aar/film-release.aar")
        }
    }
}