package com.flixclusive.core.presentation.player.extensions

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.presentation.player.R
import com.flixclusive.core.presentation.player.ui.PiPEvent
import com.flixclusive.core.presentation.player.ui.state.PlayPauseButtonState
import com.flixclusive.core.drawables.R as UiCommonR

const val ACTION_PIP_CONTROL = "player_pip_control"
const val PLAYER_PIP_EVENT = "player_pip_event"

@RequiresApi(Build.VERSION_CODES.O)
private fun Activity.getRemoteAction(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    event: PiPEvent,
): RemoteAction {
    val text = getString(label)
    return RemoteAction(
        // icon =
        Icon.createWithResource(this, icon),
        // title =
        text,
        // contentDescription =
        text,
        // intent =
        getPendingIntent(event.ordinal),
    )
}

private fun Activity.getPendingIntent(event: Int): PendingIntent {
    return PendingIntent.getBroadcast(
        // context =
        this,
        // requestCode =
        event,
        // intent =
        Intent(ACTION_PIP_CONTROL).apply {
            putExtra(PLAYER_PIP_EVENT, event)
            setPackage(packageName)
        },
        PendingIntent.FLAG_IMMUTABLE,
    )
}

@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.O)
fun Activity.updatePiPParams(
    playPauseState: PlayPauseButtonState,
    seekAmount: Long,
) {
    val params = createPiPParams(
        playPauseState = playPauseState,
        seekAmount = seekAmount,
    ).build()

    setPictureInPictureParams(params)
}

@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.O)
fun Activity.createPiPParams(
    playPauseState: PlayPauseButtonState,
    seekAmount: Long,
): PictureInPictureParams.Builder {
    val width = 16
    val height = 9

    val (backwardSeekIcon, forwardSeekIcon) = when (seekAmount) {
        5000L -> R.drawable.round_replay_5_24 to R.drawable.forward_5_black_24dp
        10000L -> R.drawable.replay_10_black_24dp to R.drawable.round_forward_10_24
        else -> R.drawable.replay_30_black_24dp to R.drawable.forward_30_black_24dp
    }

    val (playPauseIcon, eventCode) = when {
        playPauseState.isBuffering -> R.drawable.pip_loading_icon to PiPEvent.PAUSE
        playPauseState.showPlay -> UiCommonR.drawable.play to PiPEvent.PLAY
        else -> R.drawable.pause to PiPEvent.PAUSE
    }

    val actions = arrayListOf(
        getRemoteAction(
            icon = backwardSeekIcon,
            label = R.string.seek_backward,
            event = PiPEvent.BACKWARD,
        ),
        getRemoteAction(
            icon = playPauseIcon,
            label = R.string.play_pause,
            event = eventCode,
        ).apply {
            isEnabled = !playPauseState.isBuffering && playPauseState.isEnabled
        },
        getRemoteAction(
            icon = forwardSeekIcon,
            label = R.string.seek_forward,
            event = PiPEvent.FORWARD,
        ),
    )


    return PictureInPictureParams.Builder()
        .setAspectRatio(Rational(width, height))
        .setActions(actions)
}
