package com.flixclusive.presentation.tv.screens.film.player

import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import com.flixclusive.presentation.utils.PlayerUiUtils.LocalPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Suppress("DEPRECATION")
@Composable
fun AudioFocusManager(
    isPlaying: Boolean,
    shouldPlay: Boolean,
    playbackState: Int,
    onUpdateIsPlayingState: (Boolean) -> Unit,
    onUpdateCurrentTime: (Long) -> Unit,
    onShouldPlayChange: (Boolean) -> Unit,
) {
    val player = LocalPlayer.current
    val context = LocalContext.current as Activity
    val scope = rememberCoroutineScope()

    // Audio managers
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var afChangeListener: AudioManager.OnAudioFocusChangeListener? by remember { mutableStateOf(null) }
    var resumeOnFocusGain by remember { mutableStateOf(false) }
    var playbackDelayed by remember { mutableStateOf(false) }
    var playbackNowAuthorized by remember { mutableStateOf(false) }
    var playerTimeUpdaterJob: Job? by remember { mutableStateOf(null) }
    val focusLock = remember { Any() }

    LaunchedEffect(Unit) {
        afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (playbackDelayed || resumeOnFocusGain) {
                        synchronized(focusLock) {
                            playbackDelayed = false
                            resumeOnFocusGain = false
                        }
                        player?.play()
                        onUpdateIsPlayingState(true)
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    synchronized(focusLock) {
                        // only resume if playback is being interrupted
                        resumeOnFocusGain = shouldPlay
                        playbackDelayed = false
                    }
                    player?.pause()
                    onUpdateIsPlayingState(false)
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    synchronized(focusLock) {
                        resumeOnFocusGain = false
                        playbackDelayed = false
                    }
                    player?.pause()
                    onUpdateIsPlayingState(false)
                }
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if(isPlaying) {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()

                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(afChangeListener!!)
                    .build()

                audioManager.requestAudioFocus(focusRequest)
            } else {
                audioManager.requestAudioFocus(
                    afChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }

            synchronized(focusLock) {
                playbackNowAuthorized = when(result) {
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> false
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                        if(playerTimeUpdaterJob?.isActive == true)
                            return@synchronized

                        playerTimeUpdaterJob = scope.launch {
                            player?.run {
                                play()
                                onUpdateIsPlayingState(true)
                                while (this.isPlaying) {
                                    onUpdateCurrentTime(currentPosition)
                                    delay(1.seconds / 30)
                                }
                            }
                        }
                        onShouldPlayChange(true)
                        true
                    }
                    AudioManager.AUDIOFOCUS_REQUEST_DELAYED  -> {
                        playbackDelayed = true
                        false
                    }
                    else -> false
                }
            }
        }
    }

    LaunchedEffect(isPlaying, playbackState) {
        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if(isPlaying || playbackState == Player.STATE_BUFFERING) {
            context.window?.addFlags(keepScreenOnFlag)
        } else {
            context.window?.clearFlags(keepScreenOnFlag)
        }
    }
}