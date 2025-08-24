package com.flixclusive.core.presentation.player.ui.state

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Manages the device volume and applies loudness enhancement when the volume exceeds the maximum stream volume.
 * */
class VolumeManager(
    context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var loudnessEnhancer: LoudnessEnhancer? = null
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

    var currentVolume by mutableFloatStateOf(currentStreamVolume.toFloat())
        private set
    val currentVolumePercentage by derivedStateOf {
        (currentVolume / maxStreamVolume.toFloat())
            .coerceIn(0F, 1F)
    }
    val maxVolume
        get() = maxStreamVolume
            .times(loudnessEnhancer?.let { 2 } ?: 1)
            .toFloat()

    private val currentLoudnessGain
        get() = (currentVolume - maxStreamVolume) * (MAX_VOLUME_BOOST / maxStreamVolume)

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0F, maxVolume)

        if (currentVolume <= maxStreamVolume) {
            loudnessEnhancer?.enabled = false
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                currentVolume.toInt(),
                0,
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

        @OptIn(UnstableApi::class)
        @Composable
        fun rememberVolumeManager(player: Player): VolumeManager {
            val context = LocalContext.current
            val manager = remember { VolumeManager(context = context) }

            LaunchedEffect(player) {
                if (player is ExoPlayer) {
                    player.listen { events ->
                        if (events.containsAny(Player.EVENT_AUDIO_ATTRIBUTES_CHANGED, Player.EVENT_AUDIO_SESSION_ID)) {
                            manager.loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
                        }
                    }
                }
            }

            return manager
        }
    }
}
