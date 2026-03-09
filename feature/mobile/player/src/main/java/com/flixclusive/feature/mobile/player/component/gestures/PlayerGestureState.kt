package com.flixclusive.feature.mobile.player.component.gestures

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private const val SEEK_ACCUMULATE_TIMEOUT = 1200L

@Stable
internal class PlayerGestureState(
    val seekAmountMs: Long
) {
    var isDoubleTapSeekingForward by mutableStateOf(false)
        private set
    var isDoubleTapSeekingBackward by mutableStateOf(false)
        private set
    var seekSeconds by mutableIntStateOf(0)
        private set

    var isBrightnessSliderVisible by mutableStateOf(false)
        private set
    var isVolumeSliderVisible by mutableStateOf(false)
        private set
    var isSliding by mutableStateOf(false)
        private set
    var isDoubleTapping by mutableStateOf(false)
        private set
    var isSpeedBoosting by mutableStateOf(false)
        private set

    private var lastTapTimeMs by mutableLongStateOf(0L)

    val isGestureActive: Boolean get() = isDoubleTapping || isSliding || isSpeedBoosting

    fun onDoubleTap(isForward: Boolean) {
        val currentTime = System.currentTimeMillis()
        val seekIncrement = (seekAmountMs / 1000).toInt()
        val isSameSide = (isForward && isDoubleTapSeekingForward) || (!isForward && isDoubleTapSeekingBackward)

        if (currentTime - lastTapTimeMs < SEEK_ACCUMULATE_TIMEOUT && isSameSide) {
            seekSeconds += seekIncrement
        } else {
            seekSeconds = seekIncrement
        }

        lastTapTimeMs = currentTime
        isDoubleTapping = true

        if (isForward) {
            isDoubleTapSeekingForward = true
            isDoubleTapSeekingBackward = false
        } else {
            isDoubleTapSeekingBackward = true
            isDoubleTapSeekingForward = false
        }
    }

    fun hideSeekOverlay() {
        isDoubleTapSeekingForward = false
        isDoubleTapSeekingBackward = false
        isDoubleTapping = false
    }

    fun showBrightnessSlider() {
        isBrightnessSliderVisible = true
        isVolumeSliderVisible = false
        isSliding = true
    }

    fun showVolumeSlider() {
        isVolumeSliderVisible = true
        isBrightnessSliderVisible = false
        isSliding = true
    }

    fun hideSliders() {
        isBrightnessSliderVisible = false
        isVolumeSliderVisible = false
        isSliding = false
    }

    fun startSpeedBoost() {
        isSpeedBoosting = true
    }

    fun stopSpeedBoost() {
        isSpeedBoosting = false
    }

    companion object {
        @Composable
        fun rememberPlayerGestureState(seekAmountMs: Long): PlayerGestureState {
            return remember(seekAmountMs) { PlayerGestureState(seekAmountMs) }
        }
    }
}
