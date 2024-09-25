package com.flixclusive.feature.mobile.player.controls.gestures

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.feature.mobile.player.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

internal enum class GestureDirection(
    val initialAlignment: Alignment,
    val draggingAlignment: Alignment,
) {
    Right(
        initialAlignment = Alignment.CenterEnd,
        draggingAlignment = Alignment.CenterStart
    ),
    Left(
        initialAlignment = Alignment.CenterStart,
        draggingAlignment = Alignment.CenterEnd
    )
}

private const val DRAG_MULTIPLIER = 2F
private const val HIDE_DELAY = 1000L
private const val SEEK_ANIMATION_DELAY = 450L

@Composable
internal fun SeekerAndSliderGestures(
    modifier: Modifier = Modifier,
    direction: GestureDirection,
    areControlsVisible: Boolean,
    isDoubleTapping: MutableState<Boolean>,
    sliderValue: Float,
    @DrawableRes seekerIconId: Int,
    @DrawableRes sliderIconId: Int,
    seekAction: () -> Unit,
    slideAction: (Float) -> Unit,
    showControls: (Boolean) -> Unit,
    sliderValueRange: ClosedFloatingPointRange<Float> = 0F..1F,
) {
    val playerManager by rememberLocalPlayerManager()

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    var screenHeightPx by remember {
        mutableIntStateOf(
            with(density) {
                configuration.screenHeightDp.dp.roundToPx()
            }
        )
    }

    var alignmentToUse by remember { mutableStateOf(direction.initialAlignment) }

    val isVisible by rememberUpdatedState(areControlsVisible)
    val currentShowControls by rememberUpdatedState(showControls)

    var isSliderVisible by remember { mutableStateOf(false) }
    var value by remember { mutableFloatStateOf(sliderValue) }
    var isSeekerIconVisible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scope = rememberCoroutineScope()
    var sliderVisibilityTimeout: Job? by remember { mutableStateOf(null) }

    /*
    *
    * If the player controls pops up:
    * cancel the whole job and reset slider visibility state
    */
    LaunchedEffect(areControlsVisible) {
        alignmentToUse = if(!areControlsVisible) {
            delay(200)
            direction.draggingAlignment
        } else direction.initialAlignment

        if (sliderVisibilityTimeout?.isActive == true) {
            isSliderVisible = false
            sliderVisibilityTimeout = sliderVisibilityTimeout.run {
                cancel()
                null
            }
        }
    }

    /*
    *
    * Show slider if its being changed thru
    * other means like: hardware buttons, etc.
    */
    LaunchedEffect(sliderValue) {
        if (!areControlsVisible && value != sliderValue) {
            value = sliderValue

            if (sliderVisibilityTimeout?.isActive == true) {
                sliderVisibilityTimeout = sliderVisibilityTimeout!!.run {
                    cancel()
                    null
                }
            }

            sliderVisibilityTimeout = scope.launch {
                isSliderVisible = true

                delay(HIDE_DELAY)

                isSliderVisible = false
            }
        }
    }

    LaunchedEffect(isSeekerIconVisible) {
        if (isDoubleTapping.value) {
            delay(SEEK_ANIMATION_DELAY.
            times(1.5).toLong())
            isDoubleTapping.value = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.45F)
            .fillMaxHeight()
            .indication(
                interactionSource,
                ripple(bounded = false, radius = screenWidth.div(2F))
            )
            .onPlaced {
                screenHeightPx = it.size.height
            }
            .pointerInput(playerManager.player) {
                detectTapGestures(
                    onTap = {
                        currentShowControls(!isVisible)
                    },
                    onDoubleTap = { offset ->
                        scope.launch {
                            isDoubleTapping.value = true
                            var shouldShowControls = false
                            if (isVisible) {
                                currentShowControls(false)
                                shouldShowControls = true
                            }

                            val press = PressInteraction.Press(offset)

                            isSeekerIconVisible = true
                            interactionSource.emit(press)

                            seekAction()

                            interactionSource.emit(PressInteraction.Release(press))
                            delay(SEEK_ANIMATION_DELAY)

                            isSeekerIconVisible = false
                            currentShowControls(shouldShowControls)
                        }
                    }
                )
            }
            .pointerInput(areControlsVisible) {
                if (!areControlsVisible) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            sliderVisibilityTimeout = scope.launch {
                                delay(HIDE_DELAY)
                                isSliderVisible = false
                            }
                        },
                        onDragCancel = {
                            sliderVisibilityTimeout = scope.launch {
                                delay(HIDE_DELAY)
                                isSliderVisible = false
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()

                            val verticalAddition =
                                dragAmount * (DRAG_MULTIPLIER * sliderValueRange.endInclusive) / screenHeightPx.toFloat()

                            if (sliderVisibilityTimeout?.isActive == true) {
                                sliderVisibilityTimeout = sliderVisibilityTimeout!!.run {
                                    cancel()
                                    null
                                }
                            }

                            isSliderVisible = true

                            // Minus it because Y is inverted
                            value = max(value - verticalAddition, 0F)
                            slideAction(value)
                        }
                    )
                }
            }
            .then(modifier),
    ) {
        AnimatedVisibility(
            visible = isSeekerIconVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it * if (direction == GestureDirection.Left) 1 else -1 },
                animationSpec = tween(durationMillis = 500)
            ) + fadeIn(animationSpec = tween(durationMillis = 500)),
            exit = fadeOut(animationSpec = tween(durationMillis = 500)) + slideOutHorizontally(
                targetOffsetX = { it * if (direction == GestureDirection.Left) -1 else 1 },
                animationSpec = tween(durationMillis = 500)
            ),
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(seekerIconId),
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.Center)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PlayerVerticalSlider(
            modifier = Modifier
                .align(alignmentToUse),
            isVisible = isVisible || isSliderVisible,
            iconId = sliderIconId,
            value = sliderValue,
            valueRange = sliderValueRange,
            onValueChange = {
                if (sliderVisibilityTimeout?.isActive == true) {
                    sliderVisibilityTimeout = sliderVisibilityTimeout!!.run {
                        cancel()
                        null
                    }
                }

                sliderVisibilityTimeout = scope.launch {
                    val shouldShowSlider = !areControlsVisible
                    isSliderVisible = shouldShowSlider

                    value = it
                    slideAction(it)
                    currentShowControls(!shouldShowSlider)

                    delay(HIDE_DELAY)

                    if (shouldShowSlider) {
                        isSliderVisible = false
                    }
                }
            }
        )
    }
}

@Preview(
    device = "spec:parent=pixel_5,orientation=landscape",
    showSystemUi = true,
)
@Composable
private fun SeekerAndSliderGesturesPreview() {
    val (sliderValue, onSliderValueChange) = remember { mutableFloatStateOf(0.5F) }
    val (areControlsVisible, onChange) = remember { mutableStateOf(false) }

    FlixclusiveTheme {
        Surface {
            SeekerAndSliderGestures(
                direction = GestureDirection.Left,
                areControlsVisible = areControlsVisible,
                isDoubleTapping = remember { mutableStateOf(false) },
                sliderValue = sliderValue,
                seekerIconId = R.drawable.round_wb_sunny_24,
                sliderIconId = R.drawable.round_wb_sunny_24,
                seekAction = {  },
                slideAction = onSliderValueChange,
                showControls = onChange
            )
        }
    }
}