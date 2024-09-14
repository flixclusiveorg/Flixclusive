package com.flixclusive.core.ui.player.util

import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.flixclusive.core.ui.player.PlayerEvents
import com.flixclusive.core.ui.player.R
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

const val ACTION_PIP_CONTROL = "player_pip_control"
const val PLAYER_PIP_EVENT = "player_pip_event"

@RequiresApi(Build.VERSION_CODES.O)
private fun Activity.getRemoteAction(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    event: PlayerEvents,
): RemoteAction {
    val text = getString(label)
    return RemoteAction(
        /* icon = */ Icon.createWithResource(this, icon),
        /* title = */ text,
        /* contentDescription = */ text,
        /* intent = */ getPendingIntent(event.ordinal)
    )
}

private fun Activity.getPendingIntent(event: Int): PendingIntent {
    return PendingIntent.getBroadcast(
        /* context = */ this,
        /* requestCode = */ event,
        /* intent = */ Intent(ACTION_PIP_CONTROL).apply {
            putExtra(PLAYER_PIP_EVENT, event)
        },
        /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

@TargetApi(Build.VERSION_CODES.O)
fun Activity.updatePiPParams(
    isPlaying: Boolean,
    hasEnded: Boolean,
    preferredSeekIncrement: Long,
): PictureInPictureParams {
    val params = with(PictureInPictureParams.Builder()) {
        val width = 16
        val height = 9
        setAspectRatio(Rational(width, height))

        val (backwardSeekIcon, forwardSeekIcon) = when (preferredSeekIncrement) {
            5000L -> R.drawable.round_replay_5_24 to R.drawable.forward_5_black_24dp
            10000L -> R.drawable.replay_10_black_24dp to R.drawable.round_forward_10_24
            else -> R.drawable.replay_30_black_24dp to R.drawable.forward_30_black_24dp
        }

        val (playPauseIcon, eventCode) = when {
            hasEnded -> R.drawable.round_replay_24 to PlayerEvents.REPLAY
            isPlaying -> R.drawable.pause to PlayerEvents.PAUSE
            else -> UiCommonR.drawable.play to PlayerEvents.PLAY
        }

        val actions = arrayListOf(
            getRemoteAction(
                icon = backwardSeekIcon,
                label = LocaleR.string.backward_button_content_description,
                event = PlayerEvents.FORWARD
            ),
            getRemoteAction(
                icon = playPauseIcon,
                label = LocaleR.string.play_pause_button_content_description,
                event = eventCode
            ),
            getRemoteAction(
                icon = forwardSeekIcon,
                label = LocaleR.string.forward_button_content_description,
                event = PlayerEvents.FORWARD
            ).also {
                it.isEnabled = !hasEnded
            },
        )

        setActions(actions)
        build()
    }

    setPictureInPictureParams(params)
    return params
}
