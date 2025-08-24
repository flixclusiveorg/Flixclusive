package com.flixclusive.core.presentation.player.util.internal

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.flixclusive.core.datastore.model.user.NO_LIMIT_PLAYER_CACHE_SIZE
import com.flixclusive.core.util.exception.safeCall
import java.io.File

private const val CACHE_DIR_KEY = "flixclusive_player"

/**
 *
 * A singleton manager class of [SimpleCache].
 *
 * */
@OptIn(UnstableApi::class)
internal class PlayerCacheManager(
    private val context: Context,
) {
    private var simpleCache: SimpleCache? = null

    /**
     *
     * Creates an instance of [SimpleCache].
     * If the provided [preferredDiskCacheSize] is unset, which is -1,
     * the cache will automatically use [NoOpCacheEvictor] as its evictor.
     * */
    fun getCache(preferredDiskCacheSize: Long): SimpleCache {
        if (simpleCache == null) {
            simpleCache = safeCall {
                SimpleCache(
                    // cacheDir =
                    File(
                        context.cacheDir,
                        CACHE_DIR_KEY,
                    ).also { it.deleteOnExit() },
                    // evictor =
                    if (preferredDiskCacheSize == NO_LIMIT_PLAYER_CACHE_SIZE) {
                        NoOpCacheEvictor()
                    } else {
                        LeastRecentlyUsedCacheEvictor(preferredDiskCacheSize * 1024L * 1024L)
                    },
                    // databaseProvider =
                    StandaloneDatabaseProvider(context),
                )
            }
        }

        return simpleCache!!
    }

    /**
     *
     * Simply releases the cache.
     * */
    fun releaseCache() {
        simpleCache?.release()
        simpleCache = null
    }
}
