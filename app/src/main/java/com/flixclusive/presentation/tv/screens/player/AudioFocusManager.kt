package com.flixclusive.presentation.tv.screens.player

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
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.rememberLocalPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@Composable
fun AudioFocusManager() {
    val player = rememberLocalPlayer()
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
                        player.play()
                        player.playWhenReady = true
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    synchronized(focusLock) {
                        // only resume if playback is being interrupted
                        resumeOnFocusGain = player.playWhenReady
                        playbackDelayed = false
                    }
                    player.pause()
                    player.playWhenReady = false
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    synchronized(focusLock) {
                        resumeOnFocusGain = false
                        playbackDelayed = false
                    }
                    player.pause()
                    player.playWhenReady = false
                }
            }
        }
    }

    LaunchedEffect(player.isPlaying) {
        if(player.isPlaying) {
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
                            player.run {
                                playWhenReady = true
                                observePlayerPosition()
                            }
                        }
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

    LaunchedEffect(player.isPlaying, player.playbackState) {
        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if(player.isPlaying || player.playbackState == Player.STATE_BUFFERING) {
            context.window?.addFlags(keepScreenOnFlag)
        } else {
            context.window?.clearFlags(keepScreenOnFlag)
        }
    }
}