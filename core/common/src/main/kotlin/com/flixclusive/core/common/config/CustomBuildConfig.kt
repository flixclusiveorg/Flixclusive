package com.flixclusive.core.common.config

/**
 *
 * Substitute model for BuildConfig
 * */
data class CustomBuildConfig(
    val applicationName: String,
    val applicationId: String,
    val versionName: String,
    val versionCode: Long,
    val commitHash: String,
    val buildType: BuildType
)
