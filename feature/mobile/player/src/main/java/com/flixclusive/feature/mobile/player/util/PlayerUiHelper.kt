package com.flixclusive.feature.mobile.player.util

import android.app.Activity
import android.content.Context
import android.media.AudioManager

internal fun Context.percentOfVolume(
    volume: Int? = null
): Float {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    return ((volume ?: currentVolume) / maxVolume.toFloat()).coerceIn(0F, 1F)
}

internal fun Activity.setBrightness(strength: Float) {
    window?.apply {
        attributes = attributes?.apply {
            screenBrightness = strength
        }
    }
}