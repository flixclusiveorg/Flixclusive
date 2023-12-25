package com.flixclusive.presentation.mobile.screens.player.controls.gestures

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.R
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.LocalPlayer
import com.flixclusive.presentation.mobile.screens.player.controls.SEEK_ANIMATION_DELAY
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

enum class GestureDirection(
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

/**
 * Returns false if the touch is on the status bar or navigation bar
 *
 * Based on: https://github.com/recloudstream/cloudstream/blob/1356a954f3c4530d7e9d701c91c79b6326de1a81/app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt#L193C3-L193C3
 * */
private fun isValidTouch(
    statusBarHeight: Int,
    screenWidth: Int,
    position: Offset
): Boolean = position.y > statusBarHeight && position.x < screenWidth

const val MINIMUM_DRAG_AMOUNT = 2F
const val DRAG_MULTIPLIER = 2F
const val HIDE_DELAY = 1000L

@Composable
fun SeekerAndSliderGestures(
    modifier: Modifier = Modifier,
    direction: GestureDirection,
    areControlsVisible: Boolean,
    sliderValue: Float,
    @DrawableRes seekerIconId: Int,
    @DrawableRes sliderIconId: Int,
    seekAction: () -> Unit,
    slideAction: (Float) -> Unit,
    showControls: (Boolean) -> Unit,
) {
    val statusBarHeight = WindowInsets.systemBars.getTop(LocalDensity.current)

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenWidthPx = with(density) { screenWidth.roundToPx() }
    val screenHeightPx = with(density) {
        configuration.screenHeightDp.dp.roundToPx()
    }

    var alignmentToUse by remember { mutableStateOf(direction.initialAlignment) }

    val isVisible by rememberUpdatedState(areControlsVisible)
    val currentShowControls by rememberUpdatedState(showControls)

    var isSliderVisible by remember { mutableStateOf(false) }
    var value by remember {
        mutableFloatStateOf(sliderValue)
    }
    var isSeeking by remember { mutableStateOf(false) }
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

    Box(
        modifier = modifier
            .fillMaxWidth(0.45F)
            .fillMaxHeight()
            .indication(
                interactionSource,
                rememberRipple(bounded = false, radius = screenWidth.div(2F))
            )
            .pointerInput(LocalPlayer.current?.getPlayer()) {
                detectTapGestures(
                    onTap = {
                        currentShowControls(!isVisible)
                    },
                    onDoubleTap = { offset ->
                        scope.launch {
                            var shouldShowControls = false
                            if (areControlsVisible) {
                                currentShowControls(false)
                                shouldShowControls = true
                            }

                            val press = PressInteraction.Press(offset)

                            isSeeking = true
                            interactionSource.emit(press)

                            seekAction()

                            interactionSource.emit(
                                PressInteraction.Release(
                                    press
                                )
                            )
                            delay(SEEK_ANIMATION_DELAY)
                            isSeeking = false

                            currentShowControls(shouldShowControls)
                        }
                    }
                )
            }
            .pointerInput(areControlsVisible) {
                if (!areControlsVisible) {
                    detectDragGestures(
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
                        onDrag = { change, dragAmount ->
                            change.consume()

                            val isSwipingUp =
                                abs(dragAmount.y * 100 / screenHeightPx) > MINIMUM_DRAG_AMOUNT
                                        && abs(dragAmount.x * 100 / screenHeightPx) < MINIMUM_DRAG_AMOUNT

                            val verticalAddition =
                                dragAmount.y * DRAG_MULTIPLIER / screenHeightPx.toFloat()

                            if (
                                isValidTouch(
                                    statusBarHeight = statusBarHeight,
                                    screenWidth = screenWidthPx,
                                    position = change.position
                                ) && isSwipingUp
                            ) {
                                if (sliderVisibilityTimeout?.isActive == true) {
                                    sliderVisibilityTimeout = sliderVisibilityTimeout!!.run {
                                        cancel()
                                        null
                                    }
                                }

                                isSliderVisible = true

                                // Minus it because Y is inverted
                                value = max(value - verticalAddition, 0F)
                                    .coerceIn(0F, 1F)

                                slideAction(value)
                            }
                        }
                    )
                }
            },
    ) {
        AnimatedVisibility(
            visible = isSeeking,
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
        PlayerSlider(
            modifier = Modifier
                .align(alignmentToUse),
            isVisible = isVisible || isSliderVisible,
            iconId = sliderIconId,
            value = sliderValue,
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
    device = "spec:parent=Realme 5,orientation=landscape",
    showSystemUi = true,
)
@Composable
fun SeekerAndSliderGesturesPreview() {
    val (sliderValue, onSliderValueChange) = remember { mutableFloatStateOf(0.5F) }
    val (areControlsVisible, onChange) = remember { mutableStateOf(false) }

    FlixclusiveMobileTheme {
        Surface {
            SeekerAndSliderGestures(
                direction = GestureDirection.Left,
                areControlsVisible = areControlsVisible,
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