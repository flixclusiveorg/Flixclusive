package com.flixclusive.core.presentation.player

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import com.flixclusive.core.presentation.player.model.CacheMediaItem
import com.flixclusive.core.presentation.player.model.CacheMediaItem.Companion.markStreamAsFailed
import com.flixclusive.core.presentation.player.model.MediaItemKey
import com.flixclusive.core.presentation.player.model.track.MediaServer
import com.flixclusive.core.presentation.player.model.track.MediaSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource
import com.flixclusive.core.presentation.player.util.MimeTypeParser
import com.flixclusive.core.presentation.player.util.MimeTypeParser.toMimeType

@OptIn(UnstableApi::class)
internal class MediaSourceManager(
    private val dataSourceFactory: AppDataSourceFactoryImpl,
) {
    private val mediaSources = mutableMapOf<MediaItemKey, CacheMediaItem>()
    private lateinit var currentKey: MediaItemKey

    /**
     * Creates a concatenated media source with all servers, merged with subtitles.
     *
     * @param servers The list of available servers
     * @param subtitles The list of available subtitles
     *
     * @return The constructed [MediaSource]
     */
    fun createMediaSource(
        servers: List<MediaServer>,
        subtitles: List<MediaSubtitle>,
    ): MediaSource {
        val concatenatedStreams = ConcatenatingMediaSource2.Builder()

        servers.forEach { server ->
            val streamSource = createStreamMediaSource(url = server.url)

            // If subtitles exist, merge them with each server
            val finalSource = if (subtitles.isNotEmpty()) {
                val subtitleSources = subtitles.mapNotNull { createSubtitleMediaSource(it) }
                if (subtitleSources.isNotEmpty()) {
                    MergingMediaSource(streamSource, *subtitleSources.toTypedArray())
                } else {
                    streamSource
                }
            } else {
                streamSource
            }

            concatenatedStreams.add(finalSource)
        }

        return concatenatedStreams.build()
    }

    fun setCacheMediaItem(
        key: MediaItemKey,
        cacheMediaItem: CacheMediaItem,
    ) {
        mediaSources[key] = cacheMediaItem
    }

    /**
     * Sets the current active media item key.
     *
     * @param key The key of the media item to set as current
     */
    fun setCurrentKey(key: MediaItemKey) {
        when (key) {
            in mediaSources -> currentKey = key
            else -> throw IllegalArgumentException("Key $key not found in loaded media sources.")
        }
    }

    /**
     * Obtains the cache media item for a specific key.
     *
     * @param key The key of the media item to retrieve
     *
     * @return The [CacheMediaItem] if found, null otherwise
     */
    fun getCacheMediaItem(key: MediaItemKey): CacheMediaItem? {
        return mediaSources[key]
    }

    /**
     * Obtains the currently active media item.
     *
     * @return The [CacheMediaItem] if found, null otherwise
     * */
    fun getCurrentMediaItem(): CacheMediaItem? {
        return mediaSources[currentKey]
    }

    /**
     * Adds a local subtitle to the current media item if it doesn't already exist.
     * Note: This requires recreating the media source to include the new subtitle.
     *
     * @param subtitle The subtitle to add
     *
     * @return true if successfully added, false otherwise
     */
    fun addSubtitle(subtitle: MediaSubtitle): Boolean {
        val newSources = mediaSources.mapNotNull { (key, sourceData) ->
            // Since this class is singleton-scoped, ensure we only modify the current media item
            // that also has the same film/episode ID to avoid cross-contamination between different
            // media items.
            if (key.filmId != currentKey.filmId && key.episodeId != currentKey.episodeId) {
                return@mapNotNull null
            }

            // Check if subtitle already exists
            if (sourceData.hasSubtitle(subtitle)) {
                return@mapNotNull null
            }

            // Update subtitles list
            val newSubtitle = MediaSubtitle(
                url = subtitle.url,
                label =  subtitle.label,
                source = TrackSource.LOCAL,
            )

            val updatedSubtitles = sourceData.subtitles + newSubtitle
            val updatedMediaSource = createMediaSource(
                servers = sourceData.servers,
                subtitles = updatedSubtitles,
            )

            key to sourceData.copy(
                subtitles = updatedSubtitles,
                mediaSource = updatedMediaSource,
            )
        }

        if (newSources.isEmpty()) {
            return false
        }

        newSources.forEach { (key, updatedData) ->
            setCacheMediaItem(key, updatedData)
        }

        return true
    }

    /**
     * Marks a server as failed for the active media item.
     *
     * @param streamIndex The index of the server that failed
     * @return true if there are more servers to try, false if all servers have failed
     */
    fun markStreamAsFailed(streamIndex: Int) {
        val sourceData = mediaSources[currentKey] ?: return

        val updatedData = sourceData.markStreamAsFailed(streamIndex)
        setCacheMediaItem(currentKey, updatedData)
    }

    /**
     * Switches to a different server index for the active media item.
     *
     * @param index The index of the server to switch to
     * */
    fun switchStreamIndex(index: Int) {
        val sourceData = mediaSources[currentKey] ?: return

        val updatedData = sourceData.copy(currentServerIndex = index)
        setCacheMediaItem(currentKey, updatedData)
    }

    /**
     * Gets the next available server index for the active media item.
     */
    fun getNextAvailableStreamIndex(currentIndex: Int): Int? {
        val sourceData = mediaSources[currentKey] ?: return null

        // Try the next index first
        if (currentIndex + 1 in sourceData.servers.indices) {
            return currentIndex + 1
        }

        // Otherwise, find the first non-failed index
        for (i in sourceData.servers.indices) {
            if (i !in sourceData.failedStreamIndices) {
                return i
            }
        }

        return null
    }

    /**
     * Gets subtitles from the active media item as MediaSubtitle objects.
     */
    fun getSubtitles(): List<MediaSubtitle> {
        return mediaSources[currentKey]?.subtitles ?: emptyList()
    }

    /**
     * Gets servers from the active media item only.
     */
    fun getStreams(): List<MediaServer> {
        return mediaSources[currentKey]?.servers ?: emptyList()
    }

    /**
     * Creates the appropriate MediaSource based on the server URL type.
     */
    private fun createStreamMediaSource(url: String): MediaSource {
        val mediaItem = MediaItem
            .Builder()
            .setUri(url)
            .setMediaId(url)
            .build()

        val dataSourceFactory = dataSourceFactory.remote

        return when {
            MimeTypeParser.isM3U8(url) ->
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

            url.contains(".mpd", ignoreCase = true) ->
                DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

            else ->
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }
    }

    /**
     * Creates a subtitle media source.
     */
    @Suppress("DEPRECATION")
    private fun createSubtitleMediaSource(subtitle: MediaSubtitle): MediaSource? {
        if (subtitle.source == TrackSource.EMBEDDED) {
            return null
        }

        val subtitleMediaItem = MediaItem.SubtitleConfiguration
            .Builder(subtitle.url.toUri())
            .setMimeType(subtitle.toMimeType())
            .setLanguage(subtitle.label)
            .setLabel(subtitle.label)
            .build()

        val dataSourceFactory = when (subtitle.source) {
            TrackSource.REMOTE -> dataSourceFactory.remote
            else -> dataSourceFactory.local
        }

        return SingleSampleMediaSource
            .Factory(dataSourceFactory)
            .createMediaSource(subtitleMediaItem, C.TIME_UNSET)
    }
}
