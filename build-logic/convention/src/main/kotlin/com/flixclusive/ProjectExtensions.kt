package com.flixclusive

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

val Project.appId: String
    get() = libs.findVersion("app-id").get().toString()

val Project.appName: String
    get() = libs.findVersion("app-name").get().toString()

val Project.releaseVersionMajor: Int
    get() = libs.findVersion("app-version-release-major").get().toString().toInt()

val Project.releaseVersionMinor: Int
    get() = libs.findVersion("app-version-release-minor").get().toString().toInt()

val Project.releaseVersionPatch: Int
    get() = libs.findVersion("app-version-release-patch").get().toString().toInt()

val Project.releaseVersionBuild: Int
    get() = libs.findVersion("app-version-release-build").get().toString().toInt()

val Project.releaseVersionName: String
    get() = "$releaseVersionMajor.$releaseVersionMinor.$releaseVersionPatch"

val Project.releaseVersionCode: Int
    get() = releaseVersionMajor * 10000 + releaseVersionMinor * 1000 + releaseVersionPatch * 100 + releaseVersionBuild

val Project.versionPreviewCode: Int
    get() = libs.findVersion("app-version-preview").get().toString().toInt()

