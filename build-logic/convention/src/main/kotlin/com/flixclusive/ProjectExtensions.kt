package com.flixclusive

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType


val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.getCommitSha(): String {
    val gitCommitShaProvider =
        providers.exec {
            commandLine = "git rev-parse --short HEAD".split(" ")
        }

    return gitCommitShaProvider.standardOutput.asText
        .get()
        .trim()
}

fun Project.getCommitCount(): String {
    val gitCommitCountProvider =
        providers.exec {
            commandLine = "git rev-list --count HEAD".split(" ")
        }

    return gitCommitCountProvider.standardOutput.asText
        .get()
        .trim()
}
