package com.flixclusive.data.provider.repository.impl

import com.flixclusive.data.provider.repository.CacheKey
import com.flixclusive.data.provider.repository.CachedLinks
import com.flixclusive.data.provider.repository.CachedLinks.Companion.appendStream
import com.flixclusive.data.provider.repository.CachedLinks.Companion.appendSubtitle
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.data.provider.util.extensions.filterOutExpiredLinks
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// TODO: Maybe think about saving it on persistence rather than on memory?
internal class CachedLinksRepositoryImpl
    @Inject
    constructor() : CachedLinksRepository {
        private val map = HashMap<CacheKey, CachedLinks>()
        private val _caches = MutableStateFlow(map.toMap())

        override val caches: StateFlow<Map<CacheKey, CachedLinks>> = _caches.asStateFlow()

        override fun storeCache(
            key: CacheKey,
            cachedLinks: CachedLinks,
        ) {
            map[key] = cachedLinks
            map[key.getFilmOnlyKey()] = cachedLinks
            _caches.value = map.toMap()
        }

        override fun addStream(
            key: CacheKey,
            stream: Stream,
        ) {
            val newCache = map[key]?.appendStream(stream) ?: return

            map[key] = newCache
            map[key.getFilmOnlyKey()] = newCache
            _caches.value = map.toMap()
        }

        override fun addSubtitle(
            key: CacheKey,
            subtitle: Subtitle,
        ) {
            val newCache = map[key]?.appendSubtitle(subtitle) ?: return

            map[key] = newCache
            map[key.getFilmOnlyKey()] = newCache
            _caches.value = map.toMap()
        }

        override fun removeCache(key: CacheKey) {
            if (map.remove(key.getFilmOnlyKey()) == null) return
            if (map.remove(key) == null) return

            _caches.value = map.toMap()
        }

        override fun observeCache(
            key: CacheKey,
            defaultValue: CachedLinks,
        ): Flow<CachedLinks?> {
            if (!map.contains(key)) storeCache(key, defaultValue)

            return _caches
                .map {
                    val cache = it[key] ?: return@map null

                    val validStreams = cache.streams.filterOutExpiredLinks()
                    if (validStreams.isEmpty()) return@map null

                    cache.copy(streams = validStreams)
                }.distinctUntilChanged()
        }

        override fun getCache(key: CacheKey): CachedLinks? {
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
