package com.flixclusive.core.presentation.player.model.track

import androidx.compose.runtime.Immutable

/**
 * Immutable data class representing available subtitles for the active provider.
 * This is exposed to the UI layer for subtitle selection.
 */
@Immutable
data class PlayerSubtitle(
    override val label: String,
    override val isDead: Boolean,
    val url: String,
    val source: TrackSource,
    /** Known MIME type, when the caller already knows the subtitle's real format (e.g. sniffed
     * from a downloaded file). Null falls back to [com.flixclusive.core.presentation.player.util.MimeTypeParser.toMimeType]'s
     * pre-playback guess from the URL/label. */
    val mimeType: String? = null,
) : PlayerTrack {
    override fun equals(other: Any?): Boolean {
        if (other !is PlayerSubtitle) return false
        if (!url.equals(other.url, true)) return false

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
