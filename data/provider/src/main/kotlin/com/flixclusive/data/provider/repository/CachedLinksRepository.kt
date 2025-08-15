package com.flixclusive.data.provider.repository

import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.Serializable

/**
 *
 * Combination of filmId parameter
 * and season:episode of the film
 * (if it is a tv show).
 *
 * actual format: "$filmId-$season:$episode"
 *
 * */
@JvmInline
value class CacheKey private constructor(val value: String) {
    companion object {
        const val PROVIDER_ID_DELIMITER = "::"

        fun create(
            filmId: String,
            providerId: String,
            episode: Episode?
        ) = CacheKey("$providerId::$filmId-${episode?.season}:${episode?.number}")

        fun createFilmOnlyKey(
            filmId: String,
            episode: Episode?
        ) = CacheKey("$filmId-${episode?.season}:${episode?.number}")
    }

    fun getFilmOnlyKey() = CacheKey(value.substringAfter(PROVIDER_ID_DELIMITER))
}

/**
 *
 * A data model to hold all cached [MediaLink]s extracted from a provider.
 *
 * @param watchId the watch id of the film from the used provider
 * @param providerId the id of the provider used
 * @param streams watchable links obtained from the provider
 * @param subtitles subtitle links obtained from the provider
 *
 * @see MediaLink
 * @see Stream
 * @see Subtitle
 * */
data class CachedLinks(
    val watchId: String = "",
    val providerId: String = "",
    val thumbnail: String? = null,
    val episode: Episode? = null,
    val streams: List<Stream> = emptyList(),
    val subtitles: List<Subtitle> = emptyList()
) : Serializable {
    val hasNoStreamLinks: Boolean get() = streams.isEmpty()

    companion object {
        fun CachedLinks.appendStream(stream: Stream): CachedLinks {
            val streams = streams.toMutableSet()

            if (!streams.contains(stream))
                streams.add(stream)

            return copy(streams = streams.toList())
        }

        fun CachedLinks.appendSubtitle(subtitle: Subtitle): CachedLinks {
            val subtitles = subtitles.toMutableSet()

            if (!subtitles.contains(subtitle))
                subtitles.add(subtitle)

            return copy(subtitles = subtitles.toList())
        }
    }
}

interface CachedLinksRepository {
    val caches: StateFlow<Map<CacheKey, CachedLinks>>

    fun storeCache(key: CacheKey, cachedLinks: CachedLinks)
    fun addStream(key: CacheKey, stream: Stream)
    fun addSubtitle(key: CacheKey, subtitle: Subtitle)
    fun removeCache(key: CacheKey)
    fun getCache(key: CacheKey): CachedLinks?
    fun observeCache(key: CacheKey, defaultValue: CachedLinks = CachedLinks()): Flow<CachedLinks?>
    fun clear()
}
