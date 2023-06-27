package com.flixclusive.presentation.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player

@Composable
fun LifecycleAwarePlayer(
    onEventCallback: (
        totalDuration: Long,
        currentTime: Long,
        bufferedPercentage: Int,
        isPlaying: Boolean,
        playbackState: Int,
    ) -> Unit,
    onPlaybackReady: () -> Unit,
    onPlaybackEnded: () -> Unit,
    onPlaybackIdle: () -> Unit,
    onInitialize: (Player.Listener) -> Unit,
    onReleasePlayer: () -> Unit,
) {
    val lifecycle by rememberUpdatedState(LocalLifecycleOwner.current.lifecycle)

    DisposableEffect(Unit) {
        val playerListener = object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                super.onEvents(player, events)
                onEventCallback(
                    player.duration.coerceAtLeast(0L),
                    player.currentPosition.coerceAtLeast(0L),
                    player.bufferedPercentage,
                    player.isPlaying,
                    player.playbackState
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when(playbackState) {
                    Player.STATE_READY -> onPlaybackReady()
                    Player.STATE_ENDED -> onPlaybackEnded()
                    Player.STATE_IDLE -> onPlaybackIdle()
                    else -> Unit
                }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    onInitialize(playerListener)
                }
                Lifecycle.Event.ON_STOP -> {
                    onReleasePlayer()
                }
                else -> Unit
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}