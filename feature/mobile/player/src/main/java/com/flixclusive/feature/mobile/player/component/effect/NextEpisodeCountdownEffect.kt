package com.flixclusive.feature.mobile.player.component.effect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalResources
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState
import com.flixclusive.core.presentation.player.ui.state.ScrubState
import com.flixclusive.core.presentation.player.ui.state.SnackbarCountdown
import kotlinx.coroutines.flow.distinctUntilChanged
import com.flixclusive.core.presentation.player.R as PlayerR

private const val TEN_SECONDS_MS = 10_000L

@Composable
internal fun NextEpisodeCountdownEffect(
    scrubState: ScrubState,
    snackbarState: PlayerSnackbarState,
    isPlaying: () -> Boolean,
) {
    val resources = LocalResources.current

    LaunchedEffect(scrubState, snackbarState) {
        snapshotFlow {
            val remaining = scrubState.duration - scrubState.progress
            remaining in 0..TEN_SECONDS_MS && isPlaying() && scrubState.duration > 0
        }.distinctUntilChanged()
            .collect { isTenSecondsRemaining ->
                if (isTenSecondsRemaining) {
                    snackbarState.showCountdown(
                        SnackbarCountdown(
                            valueProvider = {
                                val remainingMs = (scrubState.duration - scrubState.progress) / 1000
                                val min = minOf(remainingMs, TEN_SECONDS_MS / 1000)

                                maxOf(1, min)
                            },
                            format = { resources.getString(PlayerR.string.next_episode_in, it) }
                        )
                    )
                } else if (snackbarState.countdown != null) {
                    snackbarState.dismissCountdown()
                }
            }
    }
}
