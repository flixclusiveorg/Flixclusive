@file:Suppress("DEPRECATION")

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
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.model.track.PlayerSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource
import com.flixclusive.core.presentation.player.util.MimeTypeParser
import com.flixclusive.core.presentation.player.util.MimeTypeParser.toMimeType

@OptIn(UnstableApi::class)
class MediaSourceManager(
    private val dataSourceFactory: AppDataSourceFactory,
) {
    var currentMediaSource: MediaSource? = null

    fun createMediaSource(
        server: PlayerServer,
        subtitles: List<PlayerSubtitle>,
    ): MediaSource {
        val subtitleSources = subtitles.mapNotNull { createSubtitleMediaSource(it) }

        val video = createStreamMediaSource(url = server.url)
        return MergingMediaSource(video, *subtitleSources.toTypedArray())
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

    fun createSubtitleMediaSource(subtitle: PlayerSubtitle): MediaSource? {
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
