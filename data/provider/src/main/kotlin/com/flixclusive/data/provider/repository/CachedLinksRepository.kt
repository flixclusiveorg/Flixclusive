package com.flixclusive.data.provider.repository

import com.flixclusive.data.provider.util.extensions.filterOutExpiredLinks
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
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
value class CacheKey private constructor(
    val value: String,
) {
    companion object {
        fun create(
            filmId: String,
            providerId: String,
            episode: Episode? = null,
        ) = CacheKey("$providerId::$filmId-${episode?.season}:${episode?.number}")
    }
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
    val subtitles: List<Subtitle> = emptyList(),
) : Serializable {
    val hasNoStreamLinks: Boolean get() = streams.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (other is CachedLinks) {
            return watchId == other.watchId &&
                providerId == other.providerId &&
                thumbnail == other.thumbnail &&
                episode == other.episode
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }


    companion object {
        fun CachedLinks.appendStream(stream: Stream): CachedLinks {
            val streams = streams.toMutableSet()

            if (!streams.contains(stream)) {
                streams.add(stream)
            }

            return copy(streams = streams.toList())
        }

        fun CachedLinks.appendSubtitle(subtitle: Subtitle): CachedLinks {
            val subtitles = subtitles.toMutableSet()

            if (!subtitles.contains(subtitle)) {
                subtitles.add(subtitle)
            }

            return copy(subtitles = subtitles.toList())
        }
    }

    val hasStreamableLinks get() = streams.filterOutExpiredLinks().isNotEmpty()
}

/**
 * Repository interface for managing observable streams and subtitles
 * for films and TV shows, allowing for caching and retrieval of media links.
 * */
interface CachedLinksRepository {
    /**
     * The current cache that the UI can observe.
     *
     * @see CachedLinks
     * */
    val currentCache: StateFlow<CachedLinks?>

    /**
     * An observable map of all cached links, where the key is a [CacheKey]
     * and the value is a [CachedLinks] object.
     * */
    val caches: StateFlow<Map<CacheKey, CachedLinks>>

    /**
     * Stores the given [CachedLinks] in both the observable [caches] map
     * and the [currentCache] state flow.
     *
     * @param key The [CacheKey] to associate with the cached links.
     * @param cachedLinks The [CachedLinks] object to store.
     * */
    fun storeCache(
        key: CacheKey,
        cachedLinks: CachedLinks,
    )

    /**
     * Adds a [Stream] to the cache associated with the given [CacheKey].
     * If the cache does not exist, it will not create a new one.
     *
     * @param key The [CacheKey] to associate with the stream.
     * @param stream The [Stream] to add to the cache.
     * */
    fun addStream(
        key: CacheKey,
        stream: Stream,
    )

    /**
     * Adds a [Subtitle] to the cache associated with the given [CacheKey].
     * If the cache does not exist, it will not create a new one.
     *
     * @param key The [CacheKey] to associate with the subtitle.
     * @param subtitle The [Subtitle] to add to the cache.
     * */
    fun addSubtitle(
        key: CacheKey,
        subtitle: Subtitle,
    )

    /**
     * Removes the cache associated with the given [CacheKey].
     * This will remove both the cache in the observable [caches] map
     * and the [currentCache] state flow, if it matches it.
     *
     * @param key The [CacheKey] to remove from the cache.
     * */
    fun removeCache(key: CacheKey)

    /**
     * Observes the cache associated with the given [CacheKey].
     * If the cache does not exist, it will store and return the [defaultValue] if provided.
     *
     * If all streams in the cache are expired, it will return null.
     *
     * @param key The [CacheKey] to observe.
     * @param defaultValue The default [CachedLinks] to store if the cache does not exist.
     *
     * @return A [CachedLinks] object if it exists, or the [defaultValue] if provided.
     * */
    fun getCache(
        key: CacheKey,
        defaultValue: CachedLinks? = null,
    ): CachedLinks?

    /**
     * Clears all cached links including all streams and subtitles.
     * This will reset the [currentCache] to null and clear the [caches] map.
     * */
    fun clear()
}
