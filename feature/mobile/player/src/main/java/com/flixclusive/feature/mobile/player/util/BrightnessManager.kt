package com.flixclusive.feature.mobile.player.util

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue


internal val LocalBrightnessManager = compositionLocalOf<BrightnessManager> {
    error("BrightnessManager not provided")
}

@Composable
internal fun rememberBrightnessManager()
    = rememberUpdatedState(LocalBrightnessManager.current).value

internal class BrightnessManager(private val activity: Activity) {
    var currentBrightness by mutableFloatStateOf(activity.currentBrightness)
        private set
    val maxBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL

    fun setBrightness(brightness: Float) {
        currentBrightness = brightness.coerceIn(0F, maxBrightness)
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = currentBrightness
        activity.window.attributes = layoutParams
    }
}