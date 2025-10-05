package com.flixclusive.core.presentation.player.model

import androidx.media3.exoplayer.source.MediaSource
import com.flixclusive.core.presentation.player.model.track.MediaServer
import com.flixclusive.core.presentation.player.model.track.MediaSubtitle

data class CacheMediaItem(
    val servers: List<MediaServer>,
    val subtitles: List<MediaSubtitle>,
    val mediaSource: MediaSource,
    val currentServerIndex: Int = 0,
    val failedStreamIndices: Set<Int> = emptySet(),
) {
    val hasMoreServers get() = failedStreamIndices.size < servers.size

    fun hasSubtitle(subtitle: MediaSubtitle): Boolean {
        return subtitle in subtitles
    }

    companion object {
        fun CacheMediaItem.markStreamAsFailed(index: Int): CacheMediaItem {
            return copy(failedStreamIndices = failedStreamIndices + index)
        }
    }
}
