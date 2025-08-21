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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// TODO: Maybe think about saving it on persistence rather than on memory?
internal class CachedLinksRepositoryImpl
    @Inject
    constructor(
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

        private val _currentCache = MutableStateFlow<CachedLinks?>(null)
        override val currentCache = _currentCache
            .mapLatest {
                if (it != null) {
                    val validStreams = it.streams.filterOutExpiredLinks()
                    if (validStreams.isEmpty()) return@mapLatest null

                    it.copy(streams = validStreams)
                } else {
                    null
                }
            }.stateIn(
                scope = appDispatchers.ioScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = _currentCache.value,
            )

        override val caches: StateFlow<Map<CacheKey, CachedLinks>> = _caches.asStateFlow()

        override fun storeCache(
            key: CacheKey,
            cachedLinks: CachedLinks,
        ) {
            map[key] = cachedLinks
            _currentCache.value = cachedLinks
            _caches.value = map.toMap()
        }

        override fun addStream(
            key: CacheKey,
            stream: Stream,
        ) {
            val newCache = map[key]?.appendStream(stream) ?: return

            map[key] = newCache
            _currentCache.value = newCache
            _caches.value = map.toMap()
        }

        override fun addSubtitle(
            key: CacheKey,
            subtitle: Subtitle,
        ) {
            val newCache = map[key]?.appendSubtitle(subtitle) ?: return

            map[key] = newCache
            _currentCache.value = newCache
            _caches.value = map.toMap()
        }

        override fun removeCache(key: CacheKey) {
            val item = map.remove(key) ?: return
            if (item == _currentCache.value) {
                _currentCache.value = null
            }

            _caches.value = map.toMap()
        }

        override fun getCache(
            key: CacheKey,
            defaultValue: CachedLinks?,
        ): CachedLinks? {
            if (!map.contains(key) && defaultValue != null) storeCache(key, defaultValue)

            var cache = map[key] ?: return null

            val validStreams = cache.streams.filterOutExpiredLinks()
            if (validStreams.isEmpty()) return null

            return cache.copy(streams = validStreams)
        }

        override fun clear() {
            map.clear()
            _caches.value = map.toMap()
        }
    }
