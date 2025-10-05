package com.flixclusive.core.presentation.player.model.track

import androidx.compose.runtime.Immutable

/**
 * Immutable data class representing available subtitles for the active provider.
 * This is exposed to the UI layer for subtitle selection.
 */
@Immutable
data class MediaSubtitle(
    override val label: String,
    val url: String,
    val source: TrackSource,
) : MediaTrack {
    override fun equals(other: Any?): Boolean {
        if (other !is MediaSubtitle) return false
        if (!url.equals(other.url, true)) return false

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
