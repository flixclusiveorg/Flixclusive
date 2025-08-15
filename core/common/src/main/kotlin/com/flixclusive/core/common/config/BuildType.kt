package com.flixclusive.core.common.config

/**
 * Used to determine the build type of the application.
 *
 * This is used to differentiate between debug, release, and preview builds.
 * */
enum class BuildType {
    DEBUG,
    RELEASE,
    PREVIEW; // formerly known as "Pre-release"

    fun isDebug(): Boolean = this == DEBUG
    fun isRelease(): Boolean = this == RELEASE
    fun isPreview(): Boolean = this == PREVIEW
}
