package com.flixclusive.feature.mobile.player.component.gestures

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.flixclusive.core.presentation.common.extensions.getActivity

@Stable
class BrightnessManager(
    private val activity: Activity
) {
    var currentBrightness by mutableFloatStateOf(getInitialBrightness())
        private set

    val currentBrightnessPercentage by derivedStateOf {
        currentBrightness.coerceIn(0f, 1f)
    }

    private fun getInitialBrightness(): Float {
        val layoutParams = activity.window.attributes
        return if (layoutParams.screenBrightness < 0) {
            0.5f
        } else {
            layoutParams.screenBrightness
        }
    }

    fun setBrightness(brightness: Float) {
        currentBrightness = brightness.coerceIn(0f, 1f)
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = currentBrightness
        activity.window.attributes = layoutParams
    }

    companion object {
        @Composable
        fun rememberBrightnessManager(): BrightnessManager {
            val context = LocalContext.current
            val activity = context.getActivity<Activity>()
            return remember { BrightnessManager(activity) }
        }
    }
}


