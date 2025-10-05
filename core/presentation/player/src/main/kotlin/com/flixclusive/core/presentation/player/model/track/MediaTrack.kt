package com.flixclusive.core.presentation.player.model.track

/**
 * A sealed interface representing a media track, which can be a [MediaServer], [MediaSubtitle], or [MediaAudio].
 *
 * Each track has an index that corresponds to its position in the player's track list.
 *
 * @see MediaServer
 * @see MediaSubtitle
 * @see MediaAudio
 * */
sealed interface MediaTrack {
    /** A user-friendly label for the track, such as "1080p" for servers or "English" for subtitles. */
    val label: String
}
