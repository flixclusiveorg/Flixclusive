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
}

subprojects {
    val moduleGroup = if (group.toString().contains(rootProject.name, true).not()) {
        path.substringBeforeLast(":").replace(":", ".")
    } else ""

    group = "$group$moduleGroup"
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