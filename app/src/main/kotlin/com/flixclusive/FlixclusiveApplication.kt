package com.flixclusive

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.crash.GlobalCrashHandler
import com.flixclusive.data.configuration.AppBuild
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.provider.ProviderManager
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
internal class FlixclusiveApplication : Application(), ImageLoaderFactory {
    @Inject
    lateinit var providerManager: ProviderManager
    @Inject
    lateinit var appConfigurationManager: AppConfigurationManager
    @Inject
    lateinit var appSettingsManager: AppSettingsManager
    @Inject
    lateinit var client: OkHttpClient
    @Inject
    lateinit var scope: OkHttpClient

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        GlobalCrashHandler.initialize(applicationContext)

        appConfigurationManager.initialize(
            appBuild = AppBuild(
                applicationName = getString(R.string.app_name),
                applicationId = getString(R.string.application_id),
                debug = getString(R.string.debug_mode).toBoolean(),
                versionName = getString(R.string.version_name),
                build = getString(R.string.build).toLong(),
                commitVersion = getString(R.string.commit_version)
            )
        )

        providerManager.initialize()
    }
}