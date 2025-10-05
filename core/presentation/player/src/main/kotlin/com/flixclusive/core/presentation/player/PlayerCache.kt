package com.flixclusive.core.presentation.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.flixclusive.core.datastore.model.user.NO_LIMIT_PLAYER_CACHE_SIZE
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

private const val CACHE_DIR_KEY = "flixclusive_player"

/**
 *
 * A singleton manager class of [SimpleCache].
 *
 * */
@OptIn(UnstableApi::class)
class PlayerCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cache: SimpleCache? = null

    /**
     *
     * Creates an instance of [SimpleCache].
     * If the provided [size] is unset, which is -1,
     * the cache will automatically use [NoOpCacheEvictor] as its evictor.
     *
     * If the cache is already initialized, it will return the existing instance.
     *
     * @param size The maximum size of the cache in MB. Use -1 for no limit.
     *
     * @return An instance of [SimpleCache].
     * */
    fun get(size: Long): SimpleCache {
        if (this.cache != null) {
            return this.cache!!
        }

        return SimpleCache(
            File(context.filesDir, CACHE_DIR_KEY).also { it.deleteOnExit() },
            when (size) {
                NO_LIMIT_PLAYER_CACHE_SIZE -> NoOpCacheEvictor()
                else -> LeastRecentlyUsedCacheEvictor(size * 1024L * 1024L)
            },
            StandaloneDatabaseProvider(context),
        ).also {
            this.cache = it
        }
    }

    /**
     *
     * Simply releases the cache.
     * */
    fun release() {
        cache?.release()
        cache = null
    }
}
