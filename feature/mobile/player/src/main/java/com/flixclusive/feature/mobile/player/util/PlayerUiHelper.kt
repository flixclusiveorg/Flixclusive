package com.flixclusive.feature.mobile.player.util

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings

internal fun Context.percentOfVolume(
    volume: Int? = null
): Float {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    return ((volume ?: currentVolume) / maxVolume.toFloat()).coerceIn(0F, 1F)
}

internal fun Activity.setBrightness(
    strength: Float,
    useTrueSystemBrightness: Boolean = false
) {
    if (useTrueSystemBrightness) {
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )

            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, (strength * 255).toInt()
            )
        } catch (e: Exception) {
            setBrightness(
                strength = strength,
                useTrueSystemBrightness = false
            )
        }
    } else {
        window?.apply {
            attributes = attributes?.apply {
                screenBrightness = strength
            }
        }
    }

}

internal fun Activity.getBrightness(useTrueSystemBrightness: Boolean = true): Float {
    return if (useTrueSystemBrightness) {
        try {
            Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            ) / 255F
        } catch (e: Exception) {
            // because true system brightness requires
            // permission, this is a lazy way to check
            // as it will throw an error if we do not have it
            return getBrightness(false)
        }
    } else {
        try {
            window?.attributes?.screenBrightness ?: 0F
        } catch (e: Exception) {
            0F
        }
    }
}