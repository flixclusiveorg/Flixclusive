package com.flixclusive.core.common.config

/**
 *
 * Substitute model for BuildConfig
 * */
data class CustomBuildConfig(
    val applicationName: String,
    val applicationId: String,
    val buildType: BuildType,
    val platformType: PlatformType,
    val version: AppVersion,
)
