package com.flixclusive.feature.mobile.player.util


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.presentation.common.extensions.getActivity
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.extensions.ACTION_PIP_CONTROL
import com.flixclusive.core.presentation.player.extensions.PLAYER_PIP_EVENT
import com.flixclusive.core.presentation.player.extensions.createPiPParams
import com.flixclusive.core.presentation.player.extensions.updatePiPParams
import com.flixclusive.core.presentation.player.ui.state.PlayPauseButtonState

/**
 * Enters Picture-in-Picture mode when the user leaves the app (e.g., presses the home button).
 *
 * Requires Android Oreo (26) or up to Android R (30).
 * */
@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun AutoPipModeObserverForAndroidOToR(
    player: AppPlayer,
    isInPipMode: Boolean,
    playPauseState: PlayPauseButtonState,
    seekAmount: Long,
    onPipInvoke: () -> Unit,
) {
    val activity = LocalContext.current.getActivity<ComponentActivity>()
    val broadcastReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent,
            ) {
                val event = intent.getIntExtra(PLAYER_PIP_EVENT, -1)
                if (event == -1) return

                player.handlePiPEvent(event)
            }
        }
    }

    LaunchedEffect(
        isInPipMode,
        playPauseState.showPlay,
        playPauseState.isEnabled,
        playPauseState.isBuffering
    ) {
        if (isInPipMode) {
            ContextCompat.registerReceiver(
                activity,
                /* receiver = */ broadcastReceiver,
                /* filter = */ IntentFilter(ACTION_PIP_CONTROL),
                /* flags = */ ContextCompat.RECEIVER_NOT_EXPORTED
            )

            activity.updatePiPParams(
                playPauseState = playPauseState,
                seekAmount = seekAmount
            )
        } else {
            runCatching { activity.unregisterReceiver(broadcastReceiver) }
        }
    }

    DisposableEffect(activity) {
        val params = activity.createPiPParams(
            playPauseState = playPauseState,
            seekAmount = seekAmount
        )

        val onUserLeaveBehavior = Runnable {
            onPipInvoke()
            activity.enterPictureInPictureMode(params.build())
        }

        activity.addOnUserLeaveHintListener(onUserLeaveBehavior)

        onDispose {
            activity.removeOnUserLeaveHintListener(onUserLeaveBehavior)
            runCatching { activity.unregisterReceiver(broadcastReceiver) }
        }
    }
}
