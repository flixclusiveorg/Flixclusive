package com.flixclusive.core.ui.player.util.extensions

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import java.util.Locale

/**
 * Gets all supported formats in a list
 * */
internal fun List<Tracks.Group>.getFormats() = map { it.getFormats() }.flatten()

@OptIn(UnstableApi::class)
internal fun Tracks.Group.getFormats(): List<Format> =
    (0 until this.mediaTrackGroup.length).mapNotNull { i ->
        mediaTrackGroup.getFormat(i)
    }


@UnstableApi
internal fun Format.getName(trackType: @C.TrackType Int, index: Int): String {
    val language = language
    val label = label
    return buildString {
        if (label != null) {
            append(label)
        }

        if (isEmpty()) {
            if (trackType == C.TRACK_TYPE_TEXT) {
                append("Subtitle Track #${index + 1}")
            } else {
                append("Audio Track #${index + 1}")
            }
        }

        if (language != null && language != "und" && label == null) {
            append(": ")
            append(Locale(language).displayLanguage)
        }
    }
}
