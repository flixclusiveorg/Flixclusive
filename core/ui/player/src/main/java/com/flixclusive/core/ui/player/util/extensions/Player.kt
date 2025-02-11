package com.flixclusive.core.ui.player.util.extensions

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog

internal fun Player.switchTrack(trackType: @C.TrackType Int, trackIndex: Int) {
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
            val index = trackIndex
            val tracks = currentTracks.groups.filter { it.type == trackType }

            if (tracks.isEmpty() || index >= tracks.size) {
                errorLog("Operation failed: Invalid track index: $index")
                return
            }

            infoLog("Setting $trackTypeText track: $index")
            val trackSelectionOverride = TrackSelectionOverride(tracks[trackIndex].mediaTrackGroup, 0)

            trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(trackType, false)
                .setOverrideForType(trackSelectionOverride)
                .build()
        }
}
