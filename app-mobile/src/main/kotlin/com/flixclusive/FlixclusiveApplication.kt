package com.flixclusive

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.crash.GlobalCrashHandler
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.session.UserSessionManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
internal class FlixclusiveApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    lateinit var userSessionManager: UserSessionManager

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var appDispatchers: AppDispatchers

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

        appDispatchers.ioScope.launch {
            val users = userRepository.observeUsers().first()
            val hasOldSession = userSessionManager.hasOldSession()
            val isSingleUserApp = users.size == 1

            if (hasOldSession) {
                userSessionManager.restoreSession()
                userSessionManager.currentUser.first { it != null }
            } else if (isSingleUserApp) {
                userSessionManager.signIn(users.first())
            } else {
                userSessionManager.signOut()
            }
        }
    }
}
