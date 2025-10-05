package com.flixclusive.config

import android.content.Context
import com.flixclusive.BuildConfig
import com.flixclusive.R
import com.flixclusive.core.common.config.AppVersion
import com.flixclusive.core.common.config.BuildConfigProvider
import com.flixclusive.core.common.config.BuildType
import com.flixclusive.core.common.config.CustomBuildConfig
import com.flixclusive.core.common.config.PlatformType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class BuildConfigProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BuildConfigProvider {
    override fun get(): CustomBuildConfig {
        val buildType = BuildType.entries[BuildConfig.BUILD_TYPE]

        return CustomBuildConfig(
            applicationName = context.getString(R.string.app_name),
            applicationId = BuildConfig.APPLICATION_ID,
            buildType = buildType,
            platformType = PlatformType.MOBILE,
            version = AppVersion.from(
                buildType = buildType,
                version = BuildConfig.VERSION_NAME,
            ),
        )
    }
}
