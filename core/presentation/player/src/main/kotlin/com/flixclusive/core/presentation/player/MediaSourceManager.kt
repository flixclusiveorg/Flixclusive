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

        val video = createStreamMediaSource(server)
        return MergingMediaSource(video, *subtitleSources.toTypedArray())
    }

    private fun createStreamMediaSource(server: PlayerServer): MediaSource {
        val mediaItem = createMediaItem(server.url)

        // A non-remote server is a file already on disk (SAF content:// or file://) — never an
        // HLS/DASH manifest — so it skips URL sniffing entirely and goes straight through
        // ProgressiveMediaSource on the local data source, which is the one that can actually
        // open content:// (dataSourceFactory.remote is OkHttp-backed and cannot).
        if (server.source != TrackSource.REMOTE) {
            return ProgressiveMediaSource.Factory(dataSourceFactory.local).createMediaSource(mediaItem)
        }

        val remoteDataSourceFactory = dataSourceFactory.remote

        return when {
            MimeTypeParser.isM3U8(server.url) -> {
                HlsMediaSource.Factory(remoteDataSourceFactory).createMediaSource(mediaItem)
            }

            server.url.contains(".mpd", ignoreCase = true) -> {
                DashMediaSource.Factory(remoteDataSourceFactory).createMediaSource(mediaItem)
            }

            else -> {
                ProgressiveMediaSource.Factory(remoteDataSourceFactory).createMediaSource(mediaItem)
            }
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
            .setMimeType(subtitle.mimeType ?: subtitle.toMimeType())
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
