package com.flixclusive.core.presentation.player

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
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
class MediaSourceManager(
    private val dataSourceFactory: AppDataSourceFactory,
) {
    private val mediaSources = mutableMapOf<MediaItemKey, CacheMediaItem>()
    private lateinit var currentKey: MediaItemKey

    fun createMediaSources(
        servers: List<MediaServer>,
        subtitles: List<MediaSubtitle>,
    ): List<MediaSource> {
        val subtitleSources = subtitles.mapNotNull { createSubtitleMediaSource(it) }
        return servers.map { server ->
            val video = createStreamMediaSource(url = server.url)
            MergingMediaSource(video, *subtitleSources.toTypedArray())
        }
    }

    fun setCacheMediaItem(
        key: MediaItemKey,
        cacheMediaItem: CacheMediaItem,
    ) {
        mediaSources[key] = cacheMediaItem
    }

    fun setCurrentKey(key: MediaItemKey) {
        when (key) {
            in mediaSources -> currentKey = key
            else -> throw IllegalArgumentException("Key $key not found in loaded media sources.")
        }
    }

    fun getCacheMediaItem(key: MediaItemKey): CacheMediaItem? {
        return mediaSources[key]
    }

    fun getCurrentMediaItem(): CacheMediaItem? {
        return mediaSources[currentKey]
    }

    /**
     * Adds a local subtitle to all media sources for the current film/episode.
     * Rebuilds the full playlist of media sources per server to include the new subtitle.
     */
    fun addSubtitle(subtitle: MediaSubtitle): Boolean {
        val newSources = mediaSources.mapNotNull { (key, sourceData) ->
            // Only modify items matching the current film/episode to avoid cross-contamination
            if (key.filmId != currentKey.filmId && key.episodeId != currentKey.episodeId) {
                return@mapNotNull null
            }

            if (sourceData.hasSubtitle(subtitle)) {
                return@mapNotNull null
            }

            val newSubtitle = MediaSubtitle(
                url = subtitle.url,
                label = subtitle.label,
                source = TrackSource.LOCAL,
            )

            val updatedSubtitles = sourceData.subtitles + newSubtitle
            val updatedMediaSources = createMediaSources(
                servers = sourceData.servers,
                subtitles = updatedSubtitles,
            )

            key to sourceData.copy(
                subtitles = updatedSubtitles,
                mediaSources = updatedMediaSources,
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

    fun markStreamAsFailed(streamIndex: Int) {
        val sourceData = mediaSources[currentKey] ?: return

        val updatedData = sourceData.markStreamAsFailed(streamIndex)
        setCacheMediaItem(currentKey, updatedData)
    }

    fun switchStreamIndex(index: Int) {
        val sourceData = mediaSources[currentKey] ?: return

        val updatedData = sourceData.copy(currentServerIndex = index)
        setCacheMediaItem(currentKey, updatedData)
    }

    fun getSubtitles(): List<MediaSubtitle> {
        return mediaSources[currentKey]?.subtitles ?: emptyList()
    }

    fun getStreams(): List<MediaServer> {
        return mediaSources[currentKey]?.servers ?: emptyList()
    }

    private fun createStreamMediaSource(url: String): MediaSource {
        val mediaItem = createMediaItem(url)
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

    fun createMediaItem(url: String): MediaItem {
        return MediaItem
            .Builder()
            .setUri(url)
            .setMediaId(url)
            .build()
    }

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
