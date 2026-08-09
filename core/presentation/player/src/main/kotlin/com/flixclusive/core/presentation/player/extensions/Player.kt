package com.flixclusive.core.presentation.player.extensions

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.R
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog

internal fun AppPlayer.switchTrack(
    trackType: @C.TrackType Int,
    trackIndex: Int,
) {
    val trackTypeText = when (trackType) {
        C.TRACK_TYPE_AUDIO -> "audio"
        C.TRACK_TYPE_TEXT -> "subtitle"
        else -> throw IllegalArgumentException("Invalid track type: $trackType")
    }

    trackSelectionParameters =
        if (trackIndex < 0) {
            trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(trackType, true)
                .build()
        } else {
            val tracks = currentTracks.groups
                .filter { it.type == trackType && it.isSupported }

            if (tracks.isEmpty() || trackIndex !in tracks.indices) {
                errorLog("Invalid track index ($trackIndex) for track type $trackTypeText")
                emitError(UiText.from(R.string.invalid_track_index_for_track_type, trackIndex, trackTypeText))
                return
            }

            infoLog("Setting $trackTypeText track: $trackIndex")
            val trackSelectionOverride = TrackSelectionOverride(tracks[trackIndex].mediaTrackGroup, 0)

            trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(trackType, false)
                .setOverrideForType(trackSelectionOverride)
                .build()
        }
}

@UnstableApi
internal fun <T : Renderer> ExoPlayer.getRenderer(trackType: @C.TrackType Int): T? {
    for (i in 0 until rendererCount) {
        val rendererType = getRendererType(i)
        if (rendererType == trackType) {
            return getRenderer(i) as T?
        }
    }

    return null
}
