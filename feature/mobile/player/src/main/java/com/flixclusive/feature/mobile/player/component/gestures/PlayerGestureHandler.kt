package com.flixclusive.feature.mobile.player.component.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.extensions.noIndicationClickable
import com.flixclusive.core.presentation.player.ui.state.VolumeManager
import com.flixclusive.feature.mobile.player.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DRAG_MULTIPLIER = 2F
private const val HIDE_DELAY = 1000L
private const val SEEK_ANIMATION_DELAY = 600L

@Composable
internal fun PlayerGestureHandler(
    gestureState: PlayerGestureState,
    brightnessManager: BrightnessManager,
    volumeManager: VolumeManager,
    areControlsVisible: Boolean,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSingleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    var screenHeight by remember { mutableIntStateOf(0) }
    var dragStartBrightness by remember { mutableFloatStateOf(0f) }
    var dragStartVolume by remember { mutableFloatStateOf(0f) }

    var sliderVisibilityJob: Job? by remember { mutableStateOf(null) }
    var seekAnimationJob: Job? by remember { mutableStateOf(null) }

    val leftInteractionSource = remember { MutableInteractionSource() }
    val rightInteractionSource = remember { MutableInteractionSource() }

    LaunchedEffect(areControlsVisible) {
        if (areControlsVisible && sliderVisibilityJob?.isActive == true) {
            gestureState.hideSliders()
            sliderVisibilityJob?.cancel()
            sliderVisibilityJob = null
        }
    }

    LaunchedEffect(gestureState.seekSeconds, gestureState.isDoubleTapping) {
        if (gestureState.isDoubleTapping && gestureState.seekSeconds > 0) {
            delay(SEEK_ANIMATION_DELAY)
            gestureState.hideSeekOverlay()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                    val touchSlop = viewConfiguration.touchSlop
                    val down = awaitFirstDown(requireUnconsumed = false)

                    val longPressTriggered = withTimeoutOrNull(longPressTimeout) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.all { !it.pressed }) break
                            val moved = event.changes.any {
                                (it.position - down.position).getDistance() > touchSlop
                            }
                            if (moved) break
                        }
                    } == null

                    if (longPressTriggered) {
                        gestureState.startSpeedBoost()
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                            if (event.changes.all { !it.pressed }) break
                        }
                        gestureState.stopSpeedBoost()
                    }
                }
            }
            .noIndicationClickable { onSingleTap() }
            .onSizeChanged { size ->
                screenHeight = size.height
            }
    ) {
        GestureBox(
            interactionSource = rightInteractionSource,
            screenWidth = screenWidth,
            onSingleTap = onSingleTap,
            onDoubleTap = { offset ->
                scope.launch {
                    seekAnimationJob?.cancel()
                    seekAnimationJob = launch {
                        val press = PressInteraction.Press(offset)
                        rightInteractionSource.emit(press)

                        gestureState.onDoubleTap(isForward = true)
                        onSeekForward()

                        rightInteractionSource.emit(PressInteraction.Release(press))
                    }
                }
            },
            onDragStart = {
                dragStartBrightness = brightnessManager.currentBrightness
                gestureState.showBrightnessSlider()
            },
            onDragEnd = {
                sliderVisibilityJob?.cancel()
                sliderVisibilityJob = scope.launch {
                    delay(HIDE_DELAY)
                    gestureState.hideSliders()
                }
            },
            onVerticalDrag = { dragAmount ->
                sliderVisibilityJob?.cancel()
                val dragPercent = dragAmount * (DRAG_MULTIPLIER * brightnessManager.maxBrightness) / screenHeight
                val newBrightness = dragStartBrightness - dragPercent
                brightnessManager.setBrightness(maxOf(newBrightness, 0f))
                dragStartBrightness = brightnessManager.currentBrightness
            },
            modifier = Modifier.align(Alignment.CenterStart)
        )

        GestureBox(
            interactionSource = leftInteractionSource,
            screenWidth = screenWidth,
            onSingleTap = onSingleTap,
            onDoubleTap = { offset ->
                scope.launch {
                    seekAnimationJob?.cancel()
                    seekAnimationJob = launch {
                        val press = PressInteraction.Press(offset)
                        leftInteractionSource.emit(press)

                        gestureState.onDoubleTap(isForward = false)
                        onSeekBackward()

                        leftInteractionSource.emit(PressInteraction.Release(press))
                    }
                }
            },
            onDragStart = {
                dragStartVolume = volumeManager.currentVolume
                gestureState.showVolumeSlider()
            },
            onDragEnd = {
                sliderVisibilityJob?.cancel()
                sliderVisibilityJob = scope.launch {
                    delay(HIDE_DELAY)
                    gestureState.hideSliders()
                }
            },
            onVerticalDrag = { dragAmount ->
                sliderVisibilityJob?.cancel()
                val dragPercent = dragAmount * (DRAG_MULTIPLIER * volumeManager.maxVolume) / screenHeight
                val newVolume = dragStartVolume - dragPercent
                volumeManager.setVolume(maxOf(newVolume, 0f))
                dragStartVolume = volumeManager.currentVolume
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        DoubleTapSeekOverlay(
            isVisible = gestureState.isDoubleTapSeekingBackward,
            isForward = false,
            seekSeconds = gestureState.seekSeconds,
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
        )

        DoubleTapSeekOverlay(
            isVisible = gestureState.isDoubleTapSeekingForward,
            isForward = true,
            seekSeconds = gestureState.seekSeconds,
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        )

        PlayerVerticalSlider(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(0.6f),
                    )
                ),
            isVisible = gestureState.isBrightnessSliderVisible,
            iconPainter = {
                val currentBrightnessPercentage = brightnessManager.currentBrightnessPercentage
                val icon = when {
                    currentBrightnessPercentage > 0.8F -> R.drawable.brightness_full
                    currentBrightnessPercentage < 0.5F && currentBrightnessPercentage > 0F -> R.drawable.brightness_half
                    currentBrightnessPercentage <= 0F -> R.drawable.brightness_empty
                    else -> R.drawable.brightness_full
                }

                painterResource(icon)
            },
            value = brightnessManager.currentBrightnessPercentage,
            onValueChange = { brightnessManager.setBrightness(it) },
            valueRange = 0f..1f
        )

        PlayerVerticalSlider(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(0.6f),
                        1f to Color.Transparent,
                    )
                ),
            isVisible = gestureState.isVolumeSliderVisible,
            iconPainter = {
                val currentVolumePercentage = volumeManager.currentVolumePercentage
                val icon = when {
                    currentVolumePercentage > 0.8F -> R.drawable.volume_up_black_24dp
                    currentVolumePercentage < 0.5F && currentVolumePercentage > 0F -> R.drawable.volume_down_black_24dp
                    currentVolumePercentage <= 0F -> R.drawable.volume_off_black_24dp
                    else -> R.drawable.volume_up_black_24dp
                }

                painterResource(icon)
            },
            value = volumeManager.currentVolumePercentage,
            onValueChange = { volumeManager.setVolume(it * volumeManager.maxVolume) },
            valueRange = 0f..1f
        )
    }
}

@Composable
private fun GestureBox(
    interactionSource: MutableInteractionSource,
    screenWidth: androidx.compose.ui.unit.Dp,
    onSingleTap: () -> Unit,
    onDoubleTap: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.45f)
            .fillMaxHeight()
            .indication(
                interactionSource,
                ripple(bounded = false, radius = screenWidth.div(2f))
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset -> onDoubleTap(offset) }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        onVerticalDrag(dragAmount)
                    }
                )
            }
    )
}
