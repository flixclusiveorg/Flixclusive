package com.flixclusive.feature.mobile.player.util

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager

internal fun Context.percentOfVolume(
    volume: Int? = null
): Float {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    return ((volume ?: currentVolume) / maxVolume.toFloat()).coerceIn(0F, 1F)
}

internal val Activity.currentBrightness: Float
    get() = when (val brightness = window.attributes.screenBrightness) {
        in WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF..WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL -> brightness
        else -> Settings.System.getFloat(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255
    }