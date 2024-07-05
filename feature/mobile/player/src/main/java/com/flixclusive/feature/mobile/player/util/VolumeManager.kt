package com.flixclusive.feature.mobile.player.util

import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState

internal val LocalVolumeManager = compositionLocalOf<VolumeManager> {
    error("VolumeManager not provided")
}

@Composable
internal fun rememberVolumeManager()
    = rememberUpdatedState(LocalVolumeManager.current).value

class VolumeManager(private val audioManager: AudioManager) {
    var loudnessEnhancer: LoudnessEnhancer? = null
        set(value) {
            if (currentVolume > maxStreamVolume) {
                try {
                    value?.enabled = true
                    value?.setTargetGain(currentLoudnessGain.toInt())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            field = value
        }

    private val currentStreamVolume
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    private val maxStreamVolume
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private var currentVolume = currentStreamVolume.toFloat()
    val maxVolume
        get() = maxStreamVolume
            .times(loudnessEnhancer?.let { 2 } ?: 1)
            .toFloat()

    val currentLoudnessGain
        get() = (currentVolume - maxStreamVolume) * (MAX_VOLUME_BOOST / maxStreamVolume)
    val volumePercentage
        get() = (currentVolume / maxStreamVolume.toFloat())
            .coerceIn(0F, 1F)

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0F, maxVolume)

        if (currentVolume <= maxStreamVolume) {
            loudnessEnhancer?.enabled = false
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                currentVolume.toInt(),
                0
            )
        } else {
            try {
                loudnessEnhancer?.enabled = true
                loudnessEnhancer?.setTargetGain(currentLoudnessGain.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun increaseVolume() {
        setVolume(currentVolume + 1)
    }

    fun decreaseVolume() {
        setVolume(currentVolume - 1)
    }

    companion object {
        const val MAX_VOLUME_BOOST = 2000
    }
}