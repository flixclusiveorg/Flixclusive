package com.flixclusive.core.presentation.player.ui.state

import android.content.Context
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.presentation.player.AppPlayer
import kotlin.math.roundToInt

/**
 * Manages the device volume and applies loudness enhancement when the volume exceeds the maximum server volume.
 * */
@Stable
class VolumeManager(
    context: Context,
    val isVolumeBoosted: Boolean,
    private val player: AppPlayer,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val systemCurrentVolume
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val systemMaxVolume
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    var currentVolume by mutableFloatStateOf(systemCurrentVolume.toFloat())
        private set

    val currentVolumePercentage by derivedStateOf {
        (currentVolume / maxVolume).coerceIn(0f, 1f)
    }

    val maxVolume: Float
        get() = systemMaxVolume * (if (isVolumeBoosted) 2f else 1f)

    private val currentLoudnessGain
        get() = ((currentVolume - systemMaxVolume) / systemMaxVolume * MAX_BOOST_GAIN_MB).toInt()

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, maxVolume)

        if (currentVolume <= systemMaxVolume) {
            player.setVolumeBoosterEnabled(false)
            player.setVolumeGain(0)
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                currentVolume.roundToInt(),
                0,
            )
        } else {
            try {
                player.setVolumeBoosterEnabled(true)
                player.setVolumeGain(currentLoudnessGain)
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
        private const val MAX_BOOST_GAIN_MB = 2000

        @OptIn(UnstableApi::class)
        @Composable
        fun rememberVolumeManager(
            isVolumeBoosted: Boolean,
            player: AppPlayer,
        ): VolumeManager {
            val context = LocalContext.current

            return remember {
                VolumeManager(
                    context = context,
                    player = player,
                    isVolumeBoosted = isVolumeBoosted
                )
            }
        }
    }
}
