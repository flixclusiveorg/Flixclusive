package com.flixclusive.core.common.config

/**
 * Used to determine the build type of the application.
 *
 * This is used to differentiate between debug, release, and preview builds.
 * */
enum class BuildType {
    DEBUG,
    RELEASE,
    PREVIEW, // formerly known as "Pre-release"
    ;

    val isDebug: Boolean get() = this == DEBUG
    val isRelease: Boolean get() = this == RELEASE
    val isPreview: Boolean get() = this == PREVIEW
}
