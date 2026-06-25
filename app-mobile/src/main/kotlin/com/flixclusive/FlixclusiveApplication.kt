package com.flixclusive

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.util.network.okhttp.UserAgentManager
import com.flixclusive.crash.GlobalCrashHandler
import com.flixclusive.data.backup.work.AutoBackupScheduler
import com.flixclusive.data.database.repository.UserAuthRepository
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.provider.work.CacheCleanupScheduler
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
    lateinit var userAuthRepository: UserAuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var appDispatchers: AppDispatchers

    @Inject
    lateinit var client: OkHttpClient

    @Inject
    lateinit var autoBackupScheduler: AutoBackupScheduler

    @Inject
    lateinit var cacheCleanupScheduler: CacheCleanupScheduler

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader
            .Builder(context)
            .memoryCache {
                MemoryCache
                    .Builder()
                    .maxSizePercent(context, 0.25) // 25% of available app memory
                    .build()
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 2% of device storage
                    .build()
            }.components {
                add(OkHttpNetworkFetcherFactory(callFactory = { client }))
            }.build()
    }

    override fun onCreate() {
        super.onCreate()

        GlobalCrashHandler.initialize(applicationContext)

        autoBackupScheduler.start()
        cacheCleanupScheduler.start()

        appDispatchers.ioScope.launch {
            launch {
                // Initialize user-agents
                UserAgentManager(client).loadLatestUserAgents()
            }

            val users = userRepository.observeUsers().first()
            val hasOldSession = userAuthRepository.hasOldSession()

            if (hasOldSession) {
                userAuthRepository.restoreSession()
            } else if (users.size == 1) {
                userAuthRepository.signIn(users.first())
            } else {
                userAuthRepository.signOut()
            }
        }
    }
}
