package com.flixclusive.core.presentation.player.ui

import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.flixclusive.core.presentation.player.InternalPlayer

private class AudioFocusManager(
    private val audioManager: AudioManager,
    private val onPlay: () -> Unit,
    private val onPause: () -> Unit,
    private val onStop: () -> Unit,
) {
    // Thread-safe state management using synchronized blocks
    private val focusLock = Any()
    private var resumeOnFocusGain: Boolean = false
    private var playbackDelayed: Boolean = false

    private lateinit var audioFocusRequest: AudioFocusRequest

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> handleAudioFocusGain()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> handleTransientLoss()
            AudioManager.AUDIOFOCUS_LOSS -> handleAudioFocusLoss()
        }
    }

    private fun handleAudioFocusGain() {
        synchronized(focusLock) {
            if (playbackDelayed || resumeOnFocusGain) {
                playbackDelayed = false
                resumeOnFocusGain = false
                onPlay()
            }
        }
    }

    private fun handleTransientLoss() {
        synchronized(focusLock) {
            // Only resume if playback is being interrupted
            resumeOnFocusGain = true
            playbackDelayed = false
        }
        onPause()
    }

    private fun handleAudioFocusLoss() {
        synchronized(focusLock) {
            resumeOnFocusGain = false
            playbackDelayed = false
        }
        onStop()
    }

    @Suppress("DEPRECATION")
    fun requestAudioFocus(): AudioFocusResult {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()

            audioFocusRequest = AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }

        return synchronized(focusLock) {
            when (result) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> AudioFocusResult.GRANTED
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    playbackDelayed = true
                    AudioFocusResult.DELAYED
                }

                else -> AudioFocusResult.FAILED
            }
        }
    }

    @Suppress("DEPRECATION")
    fun abandonAudioFocus() {
        synchronized(focusLock) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
            } else {
                audioManager.abandonAudioFocus(focusChangeListener)
            }

            resumeOnFocusGain = false
            playbackDelayed = false
        }
    }
}

private enum class AudioFocusResult {
    GRANTED,
    DELAYED,
    FAILED,
}

/**
 * Composable that manages audio focus for media playback.
 *
 * Requests audio focus when playback starts and abandons it when the composable is disposed.
 * */
@Composable
fun AudioFocusManager(
    activity: Activity,
    player: InternalPlayer,
    isPlaying: Boolean,
) {
    var playbackNowAuthorized by rememberSaveable { mutableStateOf(false) }

    val audioManager = remember {
        activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val audioFocusManager = remember {
        AudioFocusManager(
            audioManager = audioManager,
            onPlay = {
                player.play()
                player.playWhenReady = true
            },
            onPause = { player.pause() },
            onStop = {
                player.pause()
                player.playWhenReady = false
            },
        )
    }

    // Handle audio focus requests when playback starts
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect

        val result = audioFocusManager.requestAudioFocus()

        playbackNowAuthorized = when (result) {
            AudioFocusResult.GRANTED -> true
            AudioFocusResult.DELAYED -> false
            AudioFocusResult.FAILED -> false
        }
    }

    // Cleanup when composable is removed
    DisposableEffect(audioFocusManager) {
        onDispose {
            audioFocusManager.abandonAudioFocus()
        }
    }
}
