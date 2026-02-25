package com.flixclusive.data.provider.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.data.provider.repository.CacheKey
import com.flixclusive.data.provider.repository.CachedLinks
import com.flixclusive.data.provider.repository.CachedLinks.Companion.appendStream
import com.flixclusive.data.provider.repository.CachedLinks.Companion.appendSubtitle
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.util.extensions.filterOutExpiredLinks
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// TODO: Maybe think about saving it on persistence rather than on memory?
internal class CachedLinksRepositoryImpl @Inject constructor(
    appDispatchers: AppDispatchers,
) : CachedLinksRepository {
    /**
     * A map to hold all cached [CachedLinks]s.
     *
     * This is for simplicity and performance reasons,
     * as we can easily access the cache by its key.
     * */
    private val map = HashMap<CacheKey, CachedLinks>()
    private val _caches = MutableStateFlow(map.toMap())

    override val caches: StateFlow<Map<CacheKey, CachedLinks>> = _caches.asStateFlow()

    private val currentCacheKey = MutableStateFlow<CacheKey?>(null)

    @OptIn(FlowPreview::class)
    override val currentCache = combine(
        currentCacheKey,
        caches.debounce(300), // Debounce to prevent emitting too many times when adding streams or subtitles
    ) { key, c ->
        if (key == null) return@combine null

        val cache = c.getOrElse(key) { null }
        if (cache == null) return@combine null

        val validStreams = cache.streams.filterOutExpiredLinks()
        if (validStreams.isEmpty()) return@combine null

        cache.copy(streams = validStreams)
    }.stateIn(
        scope = appDispatchers.ioScope,
        started = SharingStarted.Lazily,
        initialValue = null,
    )

    override fun storeCache(
        key: CacheKey,
        cachedLinks: CachedLinks,
    ) {
        map[key] = cachedLinks
        _caches.value = map.toMap()
    }

    override fun addStream(
        key: CacheKey,
        stream: Stream,
    ) {
        val newCache = map[key]?.appendStream(stream) ?: return

        map[key] = newCache
        _caches.value = map.toMap()
    }

    override fun addSubtitle(
        key: CacheKey,
        subtitle: Subtitle,
    ) {
        val newCache = map[key]?.appendSubtitle(subtitle) ?: return

        map[key] = newCache
        _caches.value = map.toMap()
    }

    override fun setCurrentCache(key: CacheKey?) {
        currentCacheKey.value = key
    }

    override fun removeCache(key: CacheKey) {
        if (currentCacheKey.value == key) {
            currentCacheKey.value = null
        }

        map.remove(key) ?: return
        _caches.value = map.toMap()
    }

    override fun getCache(
        key: CacheKey,
        defaultValue: CachedLinks?,
    ): CachedLinks? {
        if (!map.contains(key) && defaultValue != null) storeCache(key, defaultValue)

        val cache = map[key] ?: return null

        val validStreams = cache.streams.filterOutExpiredLinks()
        if (validStreams.isEmpty()) return null

        return cache.copy(streams = validStreams)
    }

    override fun observeCache(key: CacheKey, defaultValue: CachedLinks?): Flow<CachedLinks?> {
        if (!map.contains(key) && defaultValue != null) storeCache(key, defaultValue)

        return _caches
            .map {
                val cache = it[key] ?: return@map null

                val validStreams = cache.streams.filterOutExpiredLinks()
                if (validStreams.isEmpty()) return@map null

                cache.copy(streams = validStreams)
            }
            .distinctUntilChanged()
    }

    override fun clear() {
        map.clear()
        _caches.value = map.toMap()
    }
}
