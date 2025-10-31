package com.flixclusive.core.presentation.player.ui.state

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.AppPlayerImpl
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Represents specific time-based scrub events during media playback.
 * */
enum class ScrubEvent {
    EIGHTY_PERCENT_REMAINING,
    TEN_SECONDS_REMAINING,
    SCRUBBING,
    NONE;

    val isScrubbing get() = this == SCRUBBING
}

/**
 *
 * */
@Stable
class ScrubState private constructor(
    private val player: AppPlayer,
) {
    /** The current position or progress of the scrubber up towards the [duration] */
    var progress by mutableLongStateOf(player.currentPosition)
        private set

    /** The total or max value the scrubber can [progress] through */
    var duration by mutableLongStateOf(player.duration)
        private set

    /** The current position on the scrubber of amount of data buffered by the player */
    var buffered by mutableLongStateOf(player.bufferedPosition)
        private set

    /**
     * The event that determines whether if the scrubber is 80% completed or
     * there's 10 seconds left until the total [duration].
     *
     * @see ScrubEvent
     * */
    var event by mutableStateOf(ScrubEvent.NONE)
        private set

    /**
     * Called when the user starts interacting with the scrubber.
     * */
    @OptIn(UnstableApi::class)
    fun onScrubStart() {
        (player as AppPlayerImpl).exoPlayer?.let {
            // TODO: Check if this is helpful for the app
            it.isScrubbingModeEnabled = true
            event = ScrubEvent.SCRUBBING
        }
    }

    /**
     * Updates the current [progress] of the scrubber as the user moves it.
     *
     * @param position The new position to update the [progress] to.
     * */
    fun onScrubMove(position: Long) {
        player.seekTo(position)
    }

    /**
     * Called when the user stops interacting with the scrubber.
     * */
    @OptIn(UnstableApi::class)
    fun onScrubEnd() {
        (player as AppPlayerImpl).exoPlayer?.let {
            it.isScrubbingModeEnabled = false
            event = ScrubEvent.NONE
        }
    }

    internal suspend fun observe() {
        while (player.isPlaying) {
            progress = player.currentPosition
            duration = player.duration
            buffered = player.bufferedPosition

            val lessThan10Seconds = isTimeInRangeOfThreshold(10_000L)
            val done80Percent = isTimeInRangeOfThreshold(calculate80Percent())
            event = when {
                !lessThan10Seconds && done80Percent -> ScrubEvent.EIGHTY_PERCENT_REMAINING
                lessThan10Seconds -> ScrubEvent.TEN_SECONDS_REMAINING
                else -> ScrubEvent.NONE
            }

            delay(1.seconds / 30)
        }
    }

    /**
     * Checks if the current playback position is within a specified [threshold] from the end of the media.
     * */
    private fun isTimeInRangeOfThreshold(threshold: Long): Boolean {
        return (player.duration - player.currentPosition) <= threshold
    }

    /**
     * Calculates 80% of the total duration of the media.
     * */
    private fun calculate80Percent(): Long {
        val deductedAmount = (player.duration * 0.8).toLong()
        return player.duration - deductedAmount
    }

    companion object {
        /**
         * Remembers and observes the scrub state of the given [player].
         * */
        @Composable
        fun rememberScrubState(player: AppPlayer): ScrubState {
            val scrubState = remember(player) { ScrubState(player) }
            LaunchedEffect(player.isPlaying) { scrubState.observe() }
            return scrubState
        }
    }
}
