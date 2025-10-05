package com.flixclusive.core.presentation.player.model.track

/**
 * Enum class representing the source of a track.
 * */
enum class TrackSource {
    /**
     * Indicates that the subtitle is embedded within the media source file itself.
     * */
    EMBEDDED,
    /**
     * Indicates that the subtitle is provided as a separate file or stream from a remote location.
     * */
    REMOTE,
    /**
     * Indicates that the subtitle is stored locally on the user's device.
     * */
    LOCAL
}
