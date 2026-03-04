package com.flixclusive.core.presentation.player.extensions

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
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
            val tracks = currentTracks.groups.filter { it.type == trackType }

            if (tracks.isEmpty() || trackIndex !in tracks.indices) {
                errorLog("Invalid track index ($trackIndex) for track type $trackTypeText")
                _errors.tryEmit(UiText.from(R.string.invalid_track_index_for_track_type, trackIndex, trackTypeText))
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
