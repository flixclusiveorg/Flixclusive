package com.flixclusive.presentation.mobile.screens.player.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowInsetsCompat
import com.flixclusive.R

fun Context.percentOfVolume(
    volume: Int? = null
): Float {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    return ((volume ?: currentVolume) / maxVolume.toFloat()).coerceIn(0F, 1F)
}

// https://stackoverflow.com/a/72501132/19371763
@Composable
inline fun <reified Activity : ComponentActivity> Context.getActivity(): Activity {
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
        stringResource(id = R.string.null_player_context_error)
    }

    return activity
}

fun Activity.setBrightness(strength: Float) {
    window?.apply {
        attributes = attributes?.apply {
            screenBrightness = strength
        }
    }
}


/**
*
* Hide the system bars for immersive experience
* */

@Suppress("DEPRECATION")
fun Activity.toggleSystemBars(isVisible: Boolean) {
    if (SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.run {
            systemBarsBehavior =
                if(isVisible && SDK_INT >= Build.VERSION_CODES.S) {
                    WindowInsetsController.BEHAVIOR_DEFAULT
                } else if(isVisible) {
                    WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_SWIPE
                } else WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (isVisible) {
                show(WindowInsetsCompat.Type.systemBars())
            } else {
                hide(WindowInsetsCompat.Type.ime())
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    } else {
        val state = if(isVisible) {
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        } else View.SYSTEM_UI_FLAG_VISIBLE

        window.decorView.systemUiVisibility = state
    }
}