package com.flixclusive.domain.provider.fake

import com.flixclusive.data.provider.cache.CacheKey
import com.flixclusive.data.provider.cache.CachedLinks
import com.flixclusive.data.provider.cache.CachedLinksRepository
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FakeCachedLinksRepository : CachedLinksRepository {
    private val map = mutableMapOf<CacheKey, CachedLinks>()
    private val _caches = MutableStateFlow<Map<CacheKey, CachedLinks>>(emptyMap())

    override val caches: StateFlow<Map<CacheKey, CachedLinks>> = _caches.asStateFlow()

    override fun storeCache(key: CacheKey, cachedLinks: CachedLinks) {
        map[key] = cachedLinks
        map[key.getFilmOnlyKey()] = cachedLinks
        _caches.value = map.toMap()
    }

    override fun addStream(key: CacheKey, stream: Stream) {
        val currentCache = map[key] ?: return
        val updatedStreams = currentCache.streams.toMutableList()

        if (!updatedStreams.contains(stream)) {
            updatedStreams.add(stream)
        }

        val newCache = currentCache.copy(streams = updatedStreams)
        map[key] = newCache
        map[key.getFilmOnlyKey()] = newCache
        _caches.value = map.toMap()
    }

    override fun addSubtitle(key: CacheKey, subtitle: Subtitle) {
        val currentCache = map[key] ?: return
        val updatedSubtitles = currentCache.subtitles.toMutableList()

        if (!updatedSubtitles.contains(subtitle)) {
            updatedSubtitles.add(subtitle)
        }

        val newCache = currentCache.copy(subtitles = updatedSubtitles)
        map[key] = newCache
        map[key.getFilmOnlyKey()] = newCache
        _caches.value = map.toMap()
    }

    override fun removeCache(key: CacheKey) {
        map.remove(key.getFilmOnlyKey())
        map.remove(key)
        _caches.value = map.toMap()
    }

    override fun getCache(key: CacheKey): CachedLinks? = map[key]

    override fun observeCache(key: CacheKey, defaultValue: CachedLinks): Flow<CachedLinks?> {
        if (!map.contains(key)) {
            storeCache(key, defaultValue)
        }

        return _caches
            .map {
                val cache = it[key]
                if (cache?.hasNoStreamLinks == true) {
                    null
                } else {
                    cache
                }
            }
            .distinctUntilChanged()
    }

    override fun clear() {
        map.clear()
        _caches.value = emptyMap()
    }

    fun addMockCache(
        filmId: String,
        providerId: String,
        episode: Episode? = null,
        watchId: String = "test-watch-id",
        streams: List<Stream> = emptyList(),
        subtitles: List<Subtitle> = emptyList()
    ) {
        val key = CacheKey.create(filmId, providerId, episode)
        val cachedLinks = CachedLinks(
            watchId = watchId,
            providerId = providerId,
            episode = episode,
            streams = streams,
            subtitles = subtitles
        )
        storeCache(key, cachedLinks)
    }
}
