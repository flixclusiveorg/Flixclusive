package com.flixclusive

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.launchOnIO
import com.flixclusive.crash.GlobalCrashHandler
import com.flixclusive.data.configuration.AppBuild
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.domain.provider.ProviderLoaderUseCase
import com.flixclusive.domain.provider.ProviderUpdaterUseCase
import com.flixclusive.domain.user.UserSessionManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
internal class FlixclusiveApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    lateinit var appConfigurationManager: AppConfigurationManager

    @Inject
    lateinit var userSessionManager: UserSessionManager

    @Inject
    lateinit var providerLoaderUseCase: ProviderLoaderUseCase

    @Inject
    lateinit var providerUpdaterUseCase: ProviderUpdaterUseCase

    @Inject
    lateinit var client: OkHttpClient

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader
            .Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { client }))
            }.build()
    }

    override fun onCreate() {
        super.onCreate()

        GlobalCrashHandler.initialize(applicationContext)

        appConfigurationManager.initialize(
            appBuild =
                AppBuild(
                    applicationName = getString(R.string.app_name),
                    applicationId = getString(R.string.application_id),
                    debug = getString(R.string.debug_mode).toBoolean(),
                    versionName = getString(R.string.version_name),
                    build = getString(R.string.build).toLong(),
                    commitVersion = getString(R.string.commit_version),
                ),
        )

        launchOnIO {
            val hasOldSession = userSessionManager.hasOldSession()

            if (hasOldSession) {
                userSessionManager.restoreSession()
                userSessionManager.currentUser.first { it != null }

                providerLoaderUseCase.initDebugFolderToPreferences()
                providerLoaderUseCase.initFromLocal()
                providerUpdaterUseCase(notify = true)
            } else {
                userSessionManager.signOut()
            }
        }
    }
}
