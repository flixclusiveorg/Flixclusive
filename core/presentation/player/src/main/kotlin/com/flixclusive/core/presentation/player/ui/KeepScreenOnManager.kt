package com.flixclusive.core.presentation.player.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.flixclusive.core.presentation.player.ui.state.PlaybackStatus

/**
 * Manages the keep-on screen flag based on player state.
 * Keeps the screen on during playback or buffering to prevent interruptions.
 */
@Composable
fun KeepScreenOnManager(
    activity: Activity,
    isPlaying: Boolean,
    playback: PlaybackStatus,
) {
    LaunchedEffect(isPlaying, playback) {
        val shouldKeepScreenOn = isPlaying || playback == PlaybackStatus.BUFFERING

        if (shouldKeepScreenOn) {
            activity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
