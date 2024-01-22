package com.flixclusive.feature.mobile.player.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.util.R as UtilR

internal fun Context.percentOfVolume(
    volume: Int? = null
): Float {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    return ((volume ?: currentVolume) / maxVolume.toFloat()).coerceIn(0F, 1F)
}

// https://stackoverflow.com/a/72501132/19371763
@Composable
internal inline fun <reified Activity : ComponentActivity> Context.getActivity(): Activity {
    val activity = when (this) {
        is Activity -> this
        else -> {
            var context = this
            while (context is ContextWrapper) {
                context = context.baseContext
                if (context is Activity) return context
            }
            null
        }
    }

    check(activity != null) {
        stringResource(id = UtilR.string.null_player_context_error)
    }

    return activity
}

internal fun Activity.setBrightness(strength: Float) {
    window?.apply {
        attributes = attributes?.apply {
            screenBrightness = strength
        }
    }
}