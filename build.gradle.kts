// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    id("com.osacky.doctor") version "0.9.1"
    id("maven-publish")
}

// Generate a mf FAT AHH JAR!
tasks.register<Jar>("fatJar") {
    archiveBaseName.set("fat")
    archiveClassifier.set("sources")
    destinationDirectory.set(File("app/build/libs"))

    subprojects.forEach { project ->
        if (project.subprojects.size == 0) {
            val projectPath = "." + project.path.replace(":", "/")
            from("$projectPath/src/main/kotlin", "$projectPath/src/main/java")
        }
    }
}

// Generate the stubs jar for the plugins-system.
// Must only be run after the task: bundlePrereleaseClassesToCompileJar or build.
tasks.register<Jar>("generateStubsJar") {
    archiveBaseName.set("classes")
    archiveClassifier.set("")
    destinationDirectory.set(File("app/build"))

    subprojects.forEach { project ->
        if (project.subprojects.size == 0) {
            val projectPath = "." + project.path.replace(":", "/")
            val appJar = File("${projectPath}/build/intermediates/compile_app_classes_jar/prerelease/classes.jar")

            if (appJar.exists()) {
                from(zipTree(appJar)) {
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                }
            }
            else {
                from({
                    project.configurations.getByName("archives")
                        .allArtifacts.files
                        .filter { it.name.contains("prerelease") }
                        .map(::zipTree)
                        .map { bundle ->
                            zipTree(bundle.files.first { it.name.endsWith("jar") })
                        }
                }) {
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                }
            }
        }
    }
}



subprojects {
    apply(plugin = "maven-publish")

    if (subprojects.size == 0) {
        val publishingGroup = "com.github.rhenwinch"
        this.group = this.group.toString().replace(rootProject.name, publishingGroup)

        publishing {
            repositories {
                mavenLocal()

                val token = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")

                if (token != null) {
                    maven {
                        credentials {
                            username = "rhenwinch"
                            password = token
                        }
                        setUrl("https://maven.pkg.github.com/rhenwinch/flixclusive")
                    }
                }
            }
        }
    }

}