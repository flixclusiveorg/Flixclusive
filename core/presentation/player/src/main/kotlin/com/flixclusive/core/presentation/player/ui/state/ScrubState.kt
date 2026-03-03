package com.flixclusive.core.presentation.player.ui.state

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.isVisible
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.presentation.player.AppPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

@Stable
class ScrubState private constructor(
    private val player: AppPlayer,
) {
    private var progressState by mutableLongStateOf(player.currentPosition)

    val progress @FrequentlyChangingValue get() = progressState

    var duration by mutableLongStateOf(max(0, player.duration))
        private set

    var buffered by mutableLongStateOf(player.bufferedPosition)
        private set

    var isScrubbing by mutableStateOf(false)
        private set

    private var observeJob: Job? = null

    @OptIn(UnstableApi::class)
    fun onScrubStart() {
        player.exoPlayer?.let {
            player.subtitleView?.isVisible = false
            it.isScrubbingModeEnabled = true
            isScrubbing = true
        }
    }

    fun onScrubMove(position: Long) {
        progressState = position
    }

    @OptIn(UnstableApi::class)
    fun onScrubEnd() {
        player.exoPlayer?.let {
            player.subtitleView?.isVisible = true
            it.seekTo(progressState)
            it.isScrubbingModeEnabled = false
            isScrubbing = false
        }
    }

    private suspend fun observe(scope: CoroutineScope) {
        player.listen { events ->
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                observeJob?.cancel()

                observeJob = scope.launch {
                    do {
                        progressState = player.currentPosition
                        this@ScrubState.duration = max(0, player.duration)
                        buffered = player.bufferedPosition

                        delay(1.seconds / 30)
                    } while (player.isPlaying)
                }
            }
        }
    }

    companion object {
        @Composable
        fun rememberScrubState(player: AppPlayer): ScrubState {
            val scrubState = remember(player) { ScrubState(player) }
            LaunchedEffect(player) { scrubState.observe(scope = this) }
            return scrubState
        }
    }
}
