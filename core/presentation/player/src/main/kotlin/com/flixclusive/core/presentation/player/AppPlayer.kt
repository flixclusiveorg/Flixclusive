package com.flixclusive.core.presentation.player

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView
import com.flixclusive.core.presentation.player.model.CacheMediaItem
import com.flixclusive.core.presentation.player.model.MediaItemKey
import com.flixclusive.core.presentation.player.model.track.MediaServer
import com.flixclusive.core.presentation.player.model.track.MediaSubtitle

/**
 * Events that can be triggered in Picture-in-Picture (PiP) mode.
 * These events correspond to user actions such as play, pause, replay, backward, and forward.
 * */
enum class PiPEvent {
    PLAY,
    PAUSE,
    REPLAY,
    BACKWARD,
    FORWARD,
}

/**
 * A wrapper interface for [Player] that provides additional functionalities
 * such as subtitle management, server selection, and playback preparation.
 * */
interface AppPlayer :
    Player,
    SubtitleOffsetProvider {

    /** The view to attach to the UI for rendering subtitles. */
    @get:UnstableApi
    var subtitleView: SubtitleView?

    /**
     * Retrieves the currently active [CacheMediaItem].
     *
     * @return The current [CacheMediaItem] if available, null otherwise
     * */
    val currentCacheMediaItem: CacheMediaItem?

    /** Initializes the player and prepares it for playback. */
    fun initialize()

    /**
     * Loads a provider's servers and subtitles lazily.
     * If already loaded, no network call is made.
     *
     * @param key The media item key representing the provider
     * @param servers List of servers from the provider
     * @param subtitles List of subtitles from the provider
     * @param startPositionMs The position to start playback from (default is 0L)
     * @param playImmediately Whether to start playback immediately after loading
     */
    fun prepare(
        key: MediaItemKey,
        servers: List<MediaServer>,
        subtitles: List<MediaSubtitle>,
        startPositionMs: Long = 0L,
        playImmediately: Boolean = false,
    )

    /**
     * Releases the media session only.
     * */
    fun releaseMediaSession()

    /**
     * Switches to a different provider's media source.
     * Maintains current playback position.
     *
     * @param key The media item key representing the provider to switch to
     * @return true if successful, false if provider not loaded
     */
    fun switchMediaSource(key: MediaItemKey): Boolean

    /**
     * Selects a specific server by its index in the active provider.
     *
     * @param index The server index within that provider
     */
    fun selectServer(index: Int)

    /**
     * Selects a subtitle track by its index in the active provider.
     *
     * @param index The index of the subtitle, or -1 to disable subtitles
     */
    fun selectSubtitle(index: Int)

    /**
     * Adds a new subtitle track dynamically during playback.
     *
     * @param subtitle The [MediaSubtitle] to add
     * */
    fun addSubtitle(subtitle: MediaSubtitle)

    /**
     * Selects an audio track by its track ID.
     *
     * @param index The ID of the audio track from ExposedAudioTrack
     */
    fun selectAudio(index: Int)

    /**
     * Changes the delay offset of subtitles for syncing purposes
     *
     * @param offset The delay offset to apply on the subtitle cues
     * */
    fun changeSubtitleDelay(offset: Long)

    /**
     * Handles Picture-in-Picture (PiP) events such as play, pause, replay, backward, and forward.
     *
     * @param event The ordinal value of [PiPEvent] to handle.
     * */
    fun handlePiPEvent(event: Int)

    companion object {
        /**
         * A list of available playback speeds ranging from 0.25x to 2.0x in increments of 0.25.
         * */
        val availablePlaybackSpeeds = List(8) {
            0F + ((it + 1) * 0.25F)
        }

        /**
         * Calculates 15% of the [duration] and returns the remaining time.
         * This is used to determine when 80% of the video has been watched.
         *
         * @param duration Total duration in milliseconds.
         *
         * @return The threshold time in milliseconds.
         * */
        internal fun getCompletionThreshold(duration: Long): Long {
            val deductedAmount = (duration * 0.85).toLong()
            return duration - deductedAmount
        }

        /**
         * Checks if the [progress] is within the [threshold] of the [duration].
         * Default threshold is 10 seconds (10,000 milliseconds).
         *
         * @param progress Current progress in milliseconds.
         * @param duration Total duration in milliseconds.
         * @param threshold Threshold in milliseconds to check against.
         *
         * @return True if within threshold, false otherwise.
         * */
        internal fun isInThreshold(
            progress: Long,
            duration: Long,
            threshold: Long = 10_000L,
        ) = (duration - progress) <= threshold
    }
}
