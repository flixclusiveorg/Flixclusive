// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        mavenLocal()
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
    alias(libs.plugins.dokka) apply false
    id("com.osacky.doctor") version "0.9.1"
}

// Generate a mf FAT AHH JAR!
tasks.register<Jar>("fatJar") {
    archiveBaseName.set("provider-stubs")
    archiveClassifier.set("sources")
    destinationDirectory.set(File("build/stubs"))

    subprojects.forEach { project ->
        if (project.subprojects.size == 0) {
            val projectPath = "." + project.path.replace(":", "/")
            from("$projectPath/src/main/kotlin", "$projectPath/src/main/java")
        }
    }
}

/**
 *
 * Generate the stubs jar for the providers-system.
 *
 * Must only be run after the tasks:
 * - assembleRelease; and
 * - bundleReleaseClassesToCompileJar.
 * */
tasks.register<Jar>("generateStubsJar") {
    archiveBaseName.set("classes")
    archiveClassifier.set("")
    destinationDirectory.set(File("build/stubs"))
    dependsOn("fatJar")

    subprojects.forEach { project ->
        if (project.subprojects.size == 0) {
            val projectPath = "." + project.path.replace(":", "/")
            val classesJar =
                File("${projectPath}/build/intermediates/compile_app_classes_jar/release/classes.jar")

            if (classesJar.exists()) {
                from(zipTree(classesJar)) {
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                }
            } else {
                from({
                    project.configurations.getByName("archives")
                        .allArtifacts.files
                        .filter { it.name.contains("release") }
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