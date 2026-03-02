package com.flixclusive.feature.mobile.player.component.effect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState
import com.flixclusive.core.presentation.player.ui.state.ScrubState
import com.flixclusive.core.presentation.player.ui.state.SnackbarCountdown
import com.flixclusive.core.presentation.player.R as PlayerR

private const val TEN_SECONDS_MS = 10_000L

@Composable
internal fun NextEpisodeCountdownEffect(
    scrubState: ScrubState,
    snackbarState: PlayerSnackbarState,
    isPlaying: Boolean,
) {
    val resources = LocalResources.current

    val isTenSecondsRemaining by remember {
        derivedStateOf {
            val remaining = scrubState.duration - scrubState.progress
            remaining in 1..TEN_SECONDS_MS && scrubState.duration > 0
        }
    }

    LaunchedEffect(isTenSecondsRemaining, isPlaying) {
        if (isTenSecondsRemaining && isPlaying) {
            snackbarState.showCountdown(
                SnackbarCountdown(
                    valueProvider = { maxOf(1, (scrubState.duration - scrubState.progress) / 1000) },
                    format = { seconds ->
                        resources.getString(PlayerR.string.next_episode_in, seconds)
                    }
                )
            )
        } else {
            snackbarState.dismissCountdown()
        }
    }
}
