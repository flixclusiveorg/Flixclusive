package com.flixclusive.config

import android.content.Context
import com.flixclusive.BuildConfig
import com.flixclusive.R
import com.flixclusive.core.common.config.BuildConfigProvider
import com.flixclusive.core.common.config.BuildType
import com.flixclusive.core.common.config.CustomBuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext

internal class BuildConfigProviderImpl(
    @ApplicationContext private val context: Context
) : BuildConfigProvider {
    override fun get() = CustomBuildConfig(
        applicationName = context.getString(R.string.app_name),
        applicationId = BuildConfig.APPLICATION_ID,
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE.toLong(),
        commitHash = BuildConfig.COMMIT_SHA,
        buildType = BuildType.entries[BuildConfig.BUILD_TYPE]
    )
}
