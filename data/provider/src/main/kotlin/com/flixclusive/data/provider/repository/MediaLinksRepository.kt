package com.flixclusive.data.provider.repository

import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.data.provider.util.extensions.filterOutExpiredLinks
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.MediaLink.Companion.getFlagOfType
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 *
 * Combination of mediaId parameter
 * and season:episode of the media
 * (if it is a TV show).
 *
 * actual format: "$mediaId-$season:$episode"
 *
 * */
@JvmInline
value class MediaLinksCacheKey private constructor(
    val value: String,
) {
    companion object {
        fun create(
            mediaId: String,
            providerId: String,
            episode: Episode? = null,
        ) = MediaLinksCacheKey("$providerId::$mediaId-${episode?.season}:${episode?.number}")

        fun LoadLinksState.toCacheKey(
            mediaId: String,
            episode: Episode? = null,
        ): MediaLinksCacheKey? {
            val providerId = when (this) {
                is LoadLinksState.Extracting -> providerId
                is LoadLinksState.Success -> providerId
                else -> return null
            }

            return create(
                mediaId = mediaId,
                providerId = providerId,
                episode = episode,
            )
        }
    }
}

/**
 *
 * A data model to hold all cached [MediaLink]s extracted from a provider.
 *
 * @param watchId the watch id of the media from the used provider
 * @param providerId the id of the provider used
 * @param streams watchable links obtained from the provider
 * @param subtitles subtitle links obtained from the provider
 * @param failedStreamUrls a set of stream URLs that have been marked as failed
 * @param hasExtractedSuccessfully a flag indicating whether the extraction process has completed
 *
 * @see MediaLink
 * @see Stream
 * @see Subtitle
 * */
data class MediaLinks(
    val watchId: String,
    val providerId: String,
    val thumbnail: String? = null,
    val streams: List<Stream> = emptyList(),
    val subtitles: List<Subtitle> = emptyList(),
    val failedStreamUrls: Set<String> = emptySet(),
    val hasExtractedSuccessfully: Boolean = false,
) {
    val hasStreamableLinks get() = streams.filterOutExpiredLinks().isNotEmpty()

    val hasPlayableLinks get() = hasStreamableLinks
        && streams.any { it.getFlagOfType<Flag.ThirdPartyGateway>() != null }

    /** Indicates whether the cached links are ready to be used, which is true when there are valid streams available and the extraction process has finished. */
    val isReady get() = hasStreamableLinks && hasExtractedSuccessfully

    companion object {
        fun MediaLinks.markStreamAsFailed(streamUrl: String): MediaLinks {
            return copy(failedStreamUrls = failedStreamUrls + streamUrl)
        }

        fun MediaLinks.appendStream(stream: Stream): MediaLinks {
            val streams = streams.toMutableSet()

            if (!streams.contains(stream)) {
                streams.add(stream)
            }

            return copy(streams = streams.toList())
        }

        fun MediaLinks.appendSubtitle(subtitle: Subtitle): MediaLinks {
            val subtitles = subtitles.toMutableSet()

            if (!subtitles.contains(subtitle)) {
                subtitles.add(subtitle)
            }

            return copy(subtitles = subtitles.toList())
        }
    }
}

/**
 * Repository interface for managing observable streams and subtitles
 * for medias and TV shows, allowing for caching and retrieval of media links.
 * */
interface MediaLinksRepository {
    /**
     * The current observable [MediaLinks] object, which represents the currently active cache of media links.
     *
     * @see MediaLinks
     * */
    val currentObservable: StateFlow<MediaLinks?>

    /**
     * An observable map of all cached links, where the key is a [MediaLinksCacheKey]
     * and the value is a [MediaLinks] object.
     * */
    val caches: StateFlow<Map<MediaLinksCacheKey, MediaLinks>>

    /**
     * Stores the given [MediaLinks] in both the observable [caches] map
     * and the [currentObservable] state flow.
     *
     * @param key The [MediaLinksCacheKey] to associate with the cached links.
     * @param mediaLinks The [MediaLinks] object to store.
     * */
    fun insertLinks(
        key: MediaLinksCacheKey,
        mediaLinks: MediaLinks,
    )

    /**
     * Adds a [Stream] to the cache associated with the given [MediaLinksCacheKey].
     * If the cache does not exist, it will not create a new one.
     *
     * @param key The [MediaLinksCacheKey] to associate with the stream.
     * @param stream The [Stream] to add to the cache.
     * */
    fun addStream(
        key: MediaLinksCacheKey,
        stream: Stream,
    )

    /**
     * Adds a [Subtitle] to the cache associated with the given [MediaLinksCacheKey].
     * If the cache does not exist, it will not create a new one.
     *
     * @param key The [MediaLinksCacheKey] to associate with the subtitle.
     * @param subtitle The [Subtitle] to add to the cache.
     * */
    fun addSubtitle(
        key: MediaLinksCacheKey,
        subtitle: Subtitle,
    )

    /**
     * Sets the current cache to the one associated with the given [MediaLinksCacheKey].
     *
     * If the cache does not exist, it will set the [currentObservable] to null.
     *
     * @see currentObservable
     * */
    fun setCurrentObservable(key: MediaLinksCacheKey?)

    /**
     * Removes the cache associated with the given [MediaLinksCacheKey].
     * This will remove both the cache in the observable [caches] map
     * and the [currentObservable] state flow, if it matches it.
     *
     * @param key The [MediaLinksCacheKey] to remove from the cache.
     * */
    fun removeCache(key: MediaLinksCacheKey)

    /**
     * Gets the cache associated with the given [MediaLinksCacheKey].
     * If the cache does not exist, it will store and return the [defaultValue] if provided.
     *
     * If all streams in the cache are expired, it will return null.
     *
     * @param key The [MediaLinksCacheKey] to observe.
     * @param defaultValue The default [MediaLinks] to store if the cache does not exist.
     *
     * @return A [MediaLinks] object if it exists, or the [defaultValue] if provided.
     * */
    fun getLinks(
        key: MediaLinksCacheKey,
        defaultValue: MediaLinks? = null,
    ): MediaLinks?

    /**
     * Observes the cache associated with the given [MediaLinksCacheKey].
     * If the cache does not exist, it will store and return the [defaultValue] if provided.
     *
     * If all streams in the cache are expired, it will return null.
     *
     * @param key The [MediaLinksCacheKey] to observe.
     * @param defaultValue The default [MediaLinks] to store if the cache does not exist.
     *
     * @return A flow of [MediaLinks] object if it exists, or the [defaultValue] if provided.
     * */
    fun observeLinks(
        key: MediaLinksCacheKey,
        defaultValue: MediaLinks? = null,
    ): Flow<MediaLinks?>

    /**
     * Marks a stream as failed in the cache associated with the given [MediaLinksCacheKey].
     *
     * @param key The [MediaLinksCacheKey] to associate with the failed stream.
     * @param streamUrl The URL of the stream to mark as failed.
     * */
    fun markStreamAsFailed(key: MediaLinksCacheKey, streamUrl: String)

    /**
     * Clears all cached links including all streams and subtitles.
     * This will reset the [currentObservable] to null and clear the [caches] map.
     * */
    fun clear()
}
