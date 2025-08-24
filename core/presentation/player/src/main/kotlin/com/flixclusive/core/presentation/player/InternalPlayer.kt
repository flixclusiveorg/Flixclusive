package com.flixclusive.core.presentation.player

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView
import com.flixclusive.core.presentation.player.model.PlaylistItem
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode

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
 * such as subtitle management, stream selection, and playback preparation.
 * */
interface InternalPlayer :
    Player,
    SubtitleOffsetProvider {
    /** Indicates whether the tracks (audio/subs) have been initialized */
    val areTracksInitialized: Boolean

    /** Indicates whether the player has been initialized. */
    val isInitialized: Boolean

    /** The view to attach to the UI for rendering subtitles. */
    @get:UnstableApi
    var subtitleView: SubtitleView?

    /** Initializes the player and prepares it for playback. */
    fun initialize()

    /**
     * Prepares the player with the given [item] and starts playback from the specified [position].
     *
     * @param item The [PlaylistItem] to be played.
     * @param position The position (in milliseconds) from which playback should start.
     * */
    fun prepare(
        item: PlaylistItem,
        position: Long = 0L,
    )

    /**
     * Retrieves a [PlaylistItem] from the current playlist based on the provided [metadata] and optional [episode].
     *
     * @param metadata The metadata of the film.
     * @param episode The episode of the film, if applicable.
     *
     * @return The matching [PlaylistItem] if found, otherwise null.
     * */
    fun getPlaylistItem(
        metadata: FilmMetadata,
        episode: Episode? = null,
    ): PlaylistItem?

    /**
     * Selects a stream by its index from the available streams
     * and prepares the player to play it.
     *
     * @param index The index of the server to select.
     * */
    fun selectStream(index: Int)

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
