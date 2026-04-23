package com.flixclusive.data.provider.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.data.provider.repository.MediaLinks
import com.flixclusive.data.provider.repository.MediaLinks.Companion.appendStream
import com.flixclusive.data.provider.repository.MediaLinks.Companion.appendSubtitle
import com.flixclusive.data.provider.repository.MediaLinks.Companion.markStreamAsFailed
import com.flixclusive.data.provider.repository.MediaLinksCacheKey
import com.flixclusive.data.provider.repository.MediaLinksRepository
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
internal class MediaLinksRepositoryImpl @Inject constructor(
    appDispatchers: AppDispatchers,
) : MediaLinksRepository {
    /**
     * A map to hold all cached [MediaLinks]s.
     *
     * This is for simplicity and performance reasons,
     * as we can easily access the cache by its key.
     * */
    private val map = HashMap<MediaLinksCacheKey, MediaLinks>()
    private val _caches = MutableStateFlow(map.toMap())

    override val caches: StateFlow<Map<MediaLinksCacheKey, MediaLinks>> = _caches.asStateFlow()

    private val currentMediaLinksCacheKey = MutableStateFlow<MediaLinksCacheKey?>(null)

    @OptIn(FlowPreview::class)
    override val currentObservable = combine(
        currentMediaLinksCacheKey,
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

    override fun insertLinks(
        key: MediaLinksCacheKey,
        mediaLinks: MediaLinks,
    ) {
        map[key] = mediaLinks
        _caches.value = map.toMap()
    }

    override fun addStream(
        key: MediaLinksCacheKey,
        stream: Stream,
    ) {
        val newCache = map[key]?.appendStream(stream) ?: return

        map[key] = newCache
        _caches.value = map.toMap()
    }

    override fun addSubtitle(
        key: MediaLinksCacheKey,
        subtitle: Subtitle,
    ) {
        val newCache = map[key]?.appendSubtitle(subtitle) ?: return

        map[key] = newCache
        _caches.value = map.toMap()
    }

    override fun setCurrentObservable(key: MediaLinksCacheKey?) {
        currentMediaLinksCacheKey.value = key
    }

    override fun removeCache(key: MediaLinksCacheKey) {
        if (currentMediaLinksCacheKey.value == key) {
            currentMediaLinksCacheKey.value = null
        }

        map.remove(key) ?: return
        _caches.value = map.toMap()
    }

    override fun getLinks(
        key: MediaLinksCacheKey,
        defaultValue: MediaLinks?,
    ): MediaLinks? {
        if (!map.contains(key) && defaultValue != null) insertLinks(key, defaultValue)

        val cache = map[key] ?: return null

        val validStreams = cache.streams.filterOutExpiredLinks()
        if (validStreams.isEmpty()) return null

        return cache.copy(streams = validStreams)
    }

    override fun observeLinks(key: MediaLinksCacheKey, defaultValue: MediaLinks?): Flow<MediaLinks?> {
        if (!map.contains(key) && defaultValue != null) insertLinks(key, defaultValue)

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
        currentMediaLinksCacheKey.value = null
    }

    override fun markStreamAsFailed(key: MediaLinksCacheKey, streamUrl: String) {
        val newCache = map[key]?.markStreamAsFailed(streamUrl) ?: return

        map[key] = newCache
        _caches.value = map.toMap()
    }
}
