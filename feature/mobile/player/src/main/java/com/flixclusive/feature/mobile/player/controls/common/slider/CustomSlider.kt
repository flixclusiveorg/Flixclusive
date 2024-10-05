package com.flixclusive.feature.mobile.player.controls.common.slider

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.lerp
import com.flixclusive.feature.mobile.player.controls.common.slider.util.SliderTokens
import com.flixclusive.feature.mobile.player.controls.common.slider.util.toColor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Material3 Slider is not good!!!! FIX THE ORIGINAL ONE, DEVS!

/**
 * <a href="https://m3.material.io/components/sliders/overview" class="external" target="_blank">Material Design slider</a>.
 *
 * Sliders allow users to make selections from a range of values.
 *
 * It uses [CustomSliderDefaults.Thumb] and [CustomSliderDefaults.Track] as the thumb and track.
 *
 * Sliders reflect a range of values along a bar, from which users may select a single value.
 * They are ideal for adjusting settings such as volume, brightness, or applying image filters.
 *
 * ![Sliders image](https://developer.android.com/images/reference/androidx/compose/material3/sliders.png)
 *
 * @param value current value of the slider. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange callback in which value should be updated
 * @param modifier the [Modifier] to be applied to this slider
 * @param enabled controls the enabled state of this slider. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param valueRange range of values that this slider can take. The passed [value] will be coerced
 * to this range.
 * @param steps if greater than 0, specifies the amount of discrete allowable values, evenly
 * distributed across the whole value range. If 0, the slider will behave continuously and allow any
 * value from the range specified. Must not be negative.
 * @param onValueChangeFinished called when value change has ended. This should not be used to
 * update the slider value (use [onValueChange] instead), but rather to know when the user has
 * completed selecting a new value by ending a drag or a click.
 * @param colors [CustomSliderColors] that will be used to resolve the colors used for this slider in
 * different states. See [CustomSliderDefaults.colors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this slider. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this slider in different states.
 */
@Composable
internal fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    colors: CustomSliderColors = CustomSliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (CustomSliderPositions) -> Unit = {
        CustomSliderDefaults.Thumb(
            interactionSource = interactionSource,
            colors = colors,
            enabled = enabled
        )
    },
    track: @Composable (CustomSliderPositions) -> Unit = { sliderPositions ->
        CustomSliderDefaults.Track(
            colors = colors,
            enabled = enabled,
            customSliderPositions = sliderPositions
        )
    },
    onValueChangeFinished: (() -> Unit)? = null,
    seekTextComposable: @Composable (() -> Unit)? = null,
) {
    require(steps >= 0) { "steps should be >= 0" }

    SliderImpl(
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        steps = steps,
        value = value,
        valueRange = valueRange,
        thumb = thumb,
        track = track,
        seekTextComposable = seekTextComposable
    )
}

/**
 * <a href="https://m3.material.io/components/sliders/overview" class="external" target="_blank">Material Design slider</a>.
 *
 * Sliders allow users to make selections from a range of values.
 *
 * Sliders reflect a range of values along a bar, from which users may select a single value.
 * They are ideal for adjusting settings such as volume, brightness, or applying image filters.
 *
 * ![Sliders image](https://developer.android.com/images/reference/androidx/compose/material3/sliders.png)
 *
 * @param value current value of the slider. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange callback in which value should be updated
 * @param modifier the [Modifier] to be applied to this slider
 * @param enabled controls the enabled state of this slider. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param valueRange range of values that this slider can take. The passed [value] will be coerced
 * to this range.
 * @param onValueChangeFinished called when value change has ended. This should not be used to
 * update the slider value (use [onValueChange] instead), but rather to know when the user has
 * completed selecting a new value by ending a drag or a click.
 * @param colors [CustomSliderColors] that will be used to resolve the colors used for this slider in
 * different states. See [CustomSliderDefaults.colors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this slider. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this slider in different states.
 * @param thumb the thumb to be displayed on the slider, it is placed on top of the track. The lambda
 * receives a [CustomSliderPositions] which is used to obtain the current active track and the tick positions
 * if the slider is discrete.
 * @param track the track to be displayed on the slider, it is placed underneath the thumb. The lambda
 * receives a [CustomSliderPositions] which is used to obtain the current active track and the tick positions
 * if the slider is discrete.
 * @param steps if greater than 0, specifies the amount of discrete allowable values, evenly
 * distributed across the whole value range. If 0, the slider will behave continuously and allow any
 * value from the range specified. Must not be negative.
 */
@Composable
@ExperimentalMaterial3Api
internal fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: CustomSliderColors = CustomSliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (CustomSliderPositions) -> Unit = {
        CustomSliderDefaults.Thumb(
            interactionSource = interactionSource,
            colors = colors,
            enabled = enabled
        )
    },
    track: @Composable (CustomSliderPositions) -> Unit = { sliderPositions ->
        CustomSliderDefaults.Track(
            colors = colors,
            enabled = enabled,
            customSliderPositions = sliderPositions
        )
    },
    /*@IntRange(from = 0)*/
    steps: Int = 0,
) {
    require(steps >= 0) { "steps should be >= 0" }

    SliderImpl(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        thumb = thumb,
        track = track
    )
}

@Composable
private fun SliderImpl(
    modifier: Modifier,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)?,
    steps: Int,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    seekTextComposable: (@Composable () -> Unit)? = null,
    thumb: @Composable (CustomSliderPositions) -> Unit,
    track: @Composable (CustomSliderPositions) -> Unit
) {
    val onValueChangeState = rememberUpdatedState<(Float) -> Unit> {
        if (it != value) {
            onValueChange(it)
        }
    }

    val tickFractions = remember(steps) {
        stepsToTickFractions(steps)
    }

    val thumbWidth = remember { mutableFloatStateOf(ThumbWidth.value) }
    val totalWidth = remember { mutableIntStateOf(0) }

    fun scaleToUserValue(minPx: Float, maxPx: Float, offset: Float) =
        scale(minPx, maxPx, offset, valueRange.start, valueRange.endInclusive)

    fun scaleToOffset(minPx: Float, maxPx: Float, userValue: Float) =
        scale(valueRange.start, valueRange.endInclusive, userValue, minPx, maxPx)

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val rawOffset = remember { mutableFloatStateOf(scaleToOffset(0f, 0f, value)) }
    val pressOffset = remember { mutableFloatStateOf(0f) }
    val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)

    val positionFraction = calcFraction(valueRange.start, valueRange.endInclusive, coerced)
    val customSliderPositions = remember {
        CustomSliderPositions(0f..positionFraction, tickFractions)
    }
    customSliderPositions.activeRange = 0f..positionFraction
    customSliderPositions.tickFractions = tickFractions

    val draggableState = remember(valueRange) {
        SliderDraggableState {
            val maxPx = max(totalWidth.intValue - thumbWidth.floatValue / 2, 0f)
            val minPx = min(thumbWidth.floatValue / 2, maxPx)
            rawOffset.floatValue = (rawOffset.floatValue + it + pressOffset.floatValue)
            pressOffset.floatValue = 0f
            val offsetInTrack = snapValueToTick(rawOffset.floatValue, tickFractions, minPx, maxPx)
            onValueChangeState.value.invoke(scaleToUserValue(minPx, maxPx, offsetInTrack))
        }
    }

    val gestureEndAction = rememberUpdatedState {
        if (!draggableState.isDragging) {
            // check isDragging in case the change is still in progress (touch -> drag case)
            onValueChangeFinished?.invoke()
        }
    }

    val press = Modifier.sliderTapModifier(
        draggableState,
        interactionSource,
        totalWidth.intValue,
        isRtl,
        rawOffset,
        gestureEndAction,
        pressOffset,
        enabled
    )

    val drag = Modifier.draggable(
        orientation = Orientation.Horizontal,
        reverseDirection = isRtl,
        enabled = enabled,
        interactionSource = interactionSource,
        onDragStopped = { _ -> gestureEndAction.value.invoke() },
        startDragImmediately = draggableState.isDragging,
        state = draggableState
    )

    Layout(
        {
            Box(modifier = Modifier.layoutId(SliderComponents.SEEK_TEXT)) { seekTextComposable?.invoke() }
            Box(modifier = Modifier.layoutId(SliderComponents.THUMB)) { thumb(customSliderPositions) }
            Box(modifier = Modifier.layoutId(SliderComponents.TRACK)) { track(customSliderPositions) }
        },
        modifier = modifier
            .minimumInteractiveComponentSize()
            .requiredSizeIn(
                minWidth = ThumbWidth,
                minHeight = ThumbHeight,
            )
            .sliderSemantics(
                value,
                enabled,
                onValueChange,
                onValueChangeFinished,
                valueRange,
                steps
            )
            .focusable(enabled, interactionSource)
            .then(press)
            .then(drag)
    ) { measurables, constraints ->

        val seekTextPlaceable = measurables.first {
            it.layoutId == SliderComponents.SEEK_TEXT
        }.measure(constraints)

        val thumbPlaceable = measurables.first {
            it.layoutId == SliderComponents.THUMB
        }.measure(constraints)

        val trackPlaceable = measurables.first {
            it.layoutId == SliderComponents.TRACK
        }.measure(
            constraints.offset(
                horizontal = -thumbPlaceable.width
            ).copy(minHeight = 0)
        )

        val sliderWidth = thumbPlaceable.width + trackPlaceable.width
        val sliderHeight = max(trackPlaceable.height, thumbPlaceable.height)

        thumbWidth.floatValue = thumbPlaceable.width.toFloat()
        totalWidth.intValue = sliderWidth

        val trackOffsetX = thumbPlaceable.width / 2
        val thumbOffsetX = ((trackPlaceable.width) * positionFraction).roundToInt()
        val seekTextOffsetX = thumbOffsetX + thumbPlaceable.width.times(0.5).roundToInt() - (seekTextPlaceable.width / 2)

        val trackOffsetY = sliderHeight - trackPlaceable.height
        val thumbOffsetY = (sliderHeight - thumbPlaceable.height) / 2
        val seekTextOffsetY = thumbOffsetY + -(50.dp.toPx().roundToInt())

        layout(
            sliderWidth,
            sliderHeight
        ) {
            seekTextPlaceable.placeRelative(
                seekTextOffsetX,
                seekTextOffsetY
            )

            trackPlaceable.placeRelative(
                trackOffsetX,
                trackOffsetY
            )

            thumbPlaceable.placeRelative(
                thumbOffsetX,
                thumbOffsetY
            )
        }
    }
}

/**
 * Object to hold defaults used by [CustomSlider]
 */
@Stable
internal object CustomSliderDefaults {

    /**
     * Creates a [CustomSliderColors] that represents the different colors used in parts of the
     * [CustomSlider] in different states.
     *
     * For the name references below the words "active" and "inactive" are used. Active part of
     * the slider is filled with progress, so if slider's progress is 30% out of 100%, left (or
     * right in RTL) 30% of the track will be active, while the rest is inactive.
     *
     * @param thumbColor thumb color when enabled
     * @param activeTrackColor color of the track in the part that is "active", meaning that the
     * thumb is ahead of it
     * @param activeTickColor colors to be used to draw tick marks on the active track, if `steps`
     * is specified
     * @param inactiveTrackColor color of the track in the part that is "inactive", meaning that the
     * thumb is before it
     * @param inactiveTickColor colors to be used to draw tick marks on the inactive track, if
     * `steps` are specified on the Slider is specified
     * @param disabledThumbColor thumb colors when disabled
     * @param disabledActiveTrackColor color of the track in the "active" part when the Slider is
     * disabled
     * @param disabledActiveTickColor colors to be used to draw tick marks on the active track
     * when Slider is disabled and when `steps` are specified on it
     * @param disabledInactiveTrackColor color of the track in the "inactive" part when the
     * Slider is disabled
     * @param disabledInactiveTickColor colors to be used to draw tick marks on the inactive part
     * of the track when Slider is disabled and when `steps` are specified on it
     */
    @Composable
    fun colors(
        thumbColor: Color = SliderTokens.HandleColor.toColor(),
        activeTrackColor: Color = SliderTokens.ActiveTrackColor.toColor(),
        activeTickColor: Color = SliderTokens.TickMarksActiveContainerColor
            .toColor()
            .copy(alpha = SliderTokens.TickMarksActiveContainerOpacity),
        inactiveTrackColor: Color = SliderTokens.InactiveTrackColor.toColor(),
        inactiveTickColor: Color = SliderTokens.TickMarksInactiveContainerColor.toColor()
            .copy(alpha = SliderTokens.TickMarksInactiveContainerOpacity),
        disabledThumbColor: Color = SliderTokens.DisabledHandleColor
            .toColor()
            .copy(alpha = SliderTokens.DisabledHandleOpacity)
            .compositeOver(MaterialTheme.colorScheme.surface),
        disabledActiveTrackColor: Color =
            SliderTokens.DisabledActiveTrackColor
                .toColor()
                .copy(alpha = SliderTokens.DisabledActiveTrackOpacity),
        disabledActiveTickColor: Color = SliderTokens.TickMarksDisabledContainerColor
            .toColor()
            .copy(alpha = SliderTokens.TickMarksDisabledContainerOpacity),
        disabledInactiveTrackColor: Color =
            SliderTokens.DisabledInactiveTrackColor
                .toColor()
                .copy(alpha = SliderTokens.DisabledInactiveTrackOpacity),

        disabledInactiveTickColor: Color = SliderTokens.TickMarksDisabledContainerColor.toColor()
            .copy(alpha = SliderTokens.TickMarksDisabledContainerOpacity)
    ): CustomSliderColors = CustomSliderColors(
        thumbColor = thumbColor,
        activeTrackColor = activeTrackColor,
        activeTickColor = activeTickColor,
        inactiveTrackColor = inactiveTrackColor,
        inactiveTickColor = inactiveTickColor,
        disabledThumbColor = disabledThumbColor,
        disabledActiveTrackColor = disabledActiveTrackColor,
        disabledActiveTickColor = disabledActiveTickColor,
        disabledInactiveTrackColor = disabledInactiveTrackColor,
        disabledInactiveTickColor = disabledInactiveTickColor
    )

    /**
     * The Default thumb for [CustomSlider] and [RangeSlider]
     *
     * @param interactionSource the [MutableInteractionSource] representing the stream of
     * [Interaction]s for this thumb. You can create and pass in your own `remember`ed
     * instance to observe
     * @param modifier the [Modifier] to be applied to the thumb.
     * @param colors [CustomSliderColors] that will be used to resolve the colors used for this thumb in
     * different states. See [CustomSliderDefaults.colors].
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     * not respond to user input, and it will appear visually disabled and disabled to
     * accessibility services.
     */
    @Composable
    fun Thumb(
        modifier: Modifier = Modifier,
        interactionSource: MutableInteractionSource,
        isValueChanging: Boolean = true,
        colors: CustomSliderColors = colors(),
        enabled: Boolean = true,
        thumbSize: DpSize = ThumbSize
    ) {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> interactions.add(interaction)
                    is PressInteraction.Release -> interactions.remove(interaction.press)
                    is PressInteraction.Cancel -> interactions.remove(interaction.press)
                    is DragInteraction.Start -> interactions.add(interaction)
                    is DragInteraction.Stop -> interactions.remove(interaction.start)
                    is DragInteraction.Cancel -> interactions.remove(interaction.start)
                }
            }
        }

        val elevation = if (interactions.isNotEmpty()) {
            ThumbPressedElevation
        } else {
            ThumbDefaultElevation
        }
        val shape = SliderTokens.HandleShape

        AnimatedVisibility(
            visible = isValueChanging,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            Spacer(
                modifier
                    .size(thumbSize)
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple(
                            bounded = false,
                            radius = SliderTokens.StateLayerSize / 2
                        )
                    )
                    .hoverable(interactionSource = interactionSource)
                    .shadow(if (enabled) elevation else 0.dp, shape, clip = false)
                    .background(colors.thumbColor(enabled).value, shape)
            )
        }
    }


    /**
     * The Default track for [CustomSlider] and [RangeSlider]
     *
     * @param customSliderPositions [CustomSliderPositions] which is used to obtain the current active track
     * and the tick positions if the slider is discrete.
     * @param modifier the [Modifier] to be applied to the track.
     * @param colors [CustomSliderColors] that will be used to resolve the colors used for this track in
     * different states. See [CustomSliderDefaults.colors].
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     * not respond to user input, and it will appear visually disabled and disabled to
     * accessibility services.
     */
    @Composable
    fun Track(
        customSliderPositions: CustomSliderPositions,
        modifier: Modifier = Modifier,
        colors: CustomSliderColors = colors(),
        enabled: Boolean = true,
        gradient: Boolean = true,
    ) {
        val inactiveTrackColor = colors.trackColor(enabled, active = false)
        val activeTrackColor = colors.trackColor(enabled, active = true)
        val gradientedColor = if(gradient) {
            Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                )
            )
        } else null
        val inactiveTickColor = colors.tickColor(enabled, active = false)
        val activeTickColor = colors.tickColor(enabled, active = true)
        Canvas(
            modifier
                .fillMaxWidth()
                .height(50.dp) // Modified so it has BIGGER touch surface!!!! screw the original slider!!
        ) {
            val isRtl = layoutDirection == LayoutDirection.Rtl
            val sliderLeft = Offset(0f, center.y)
            val sliderRight = Offset(size.width, center.y)
            val sliderStart = if (isRtl) sliderRight else sliderLeft
            val sliderEnd = if (isRtl) sliderLeft else sliderRight
            val tickSize = TickSize.toPx()
            val trackStrokeWidth = TrackHeight.toPx()
            drawLine(
                inactiveTrackColor.value,
                sliderStart,
                sliderEnd,
                trackStrokeWidth,
                StrokeCap.Round
            )
            val sliderValueEnd = Offset(
                sliderStart.x +
                        (sliderEnd.x - sliderStart.x) * customSliderPositions.activeRange.endInclusive,
                center.y
            )

            val sliderValueStart = Offset(
                sliderStart.x +
                        (sliderEnd.x - sliderStart.x) * customSliderPositions.activeRange.start,
                center.y
            )

            if (gradientedColor != null) {
                drawLine(
                    gradientedColor,
                    sliderValueStart,
                    sliderValueEnd,
                    trackStrokeWidth,
                    StrokeCap.Round
                )
            } else {
                drawLine(
                    activeTrackColor.value,
                    sliderValueStart,
                    sliderValueEnd,
                    trackStrokeWidth,
                    StrokeCap.Round
                )
            }

            customSliderPositions.tickFractions.groupBy {
                it > customSliderPositions.activeRange.endInclusive ||
                        it < customSliderPositions.activeRange.start
            }.forEach { (outsideFraction, list) ->
                drawPoints(
                    list.map {
                        Offset(lerp(sliderStart, sliderEnd, it).x, center.y)
                    },
                    PointMode.Points,
                    (if (outsideFraction) inactiveTickColor else activeTickColor).value,
                    tickSize,
                    StrokeCap.Round
                )
            }
        }
    }
}

private fun snapValueToTick(
    current: Float,
    tickFractions: FloatArray,
    minPx: Float,
    maxPx: Float
): Float {
    // target is a closest anchor to the `current`, if exists
    return tickFractions
        .minByOrNull { abs(lerp(minPx, maxPx, it) - current) }
        ?.run { lerp(minPx, maxPx, this) }
        ?: current
}

private fun stepsToTickFractions(steps: Int): FloatArray {
    return if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }
}

// Scale x1 from a1..b1 range to a2..b2 range
private fun scale(a1: Float, b1: Float, x1: Float, a2: Float, b2: Float) =
    lerp(a2, b2, calcFraction(a1, b1, x1))

// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

private fun Modifier.sliderSemantics(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
): Modifier {
    val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
    return semantics {
        if (!enabled) disabled()
        setProgress(
            action = { targetValue ->
                var newValue = targetValue.coerceIn(valueRange.start, valueRange.endInclusive)
                val originalVal = newValue
                val resolvedValue = if (steps > 0) {
                    var distance: Float = newValue
                    for (i in 0..steps + 1) {
                        val stepValue = lerp(
                            valueRange.start,
                            valueRange.endInclusive,
                            i.toFloat() / (steps + 1)
                        )
                        if (abs(stepValue - originalVal) <= distance) {
                            distance = abs(stepValue - originalVal)
                            newValue = stepValue
                        }
                    }
                    newValue
                } else {
                    newValue
                }

                // This is to keep it consistent with AbsSeekbar.java: return false if no
                // change from current.
                if (resolvedValue == coerced) {
                    false
                } else {
                    onValueChange(resolvedValue)
                    onValueChangeFinished?.invoke()
                    true
                }
            }
        )
    }.progressSemantics(value, valueRange, steps)
}

private fun Modifier.sliderTapModifier(
    draggableState: DraggableState,
    interactionSource: MutableInteractionSource,
    maxPx: Int,
    isRtl: Boolean,
    rawOffset: State<Float>,
    gestureEndAction: State<() -> Unit>,
    pressOffset: MutableState<Float>,
    enabled: Boolean
) = composed(
    factory = {
        if (enabled) {
            val scope = rememberCoroutineScope()
            pointerInput(draggableState, interactionSource, maxPx, isRtl) {
                detectTapGestures(
                    onPress = { pos ->
                        val to = if (isRtl) maxPx - pos.x else pos.x
                        pressOffset.value = to - rawOffset.value
                        try {
                            awaitRelease()
                        } catch (_: GestureCancellationException) {
                            pressOffset.value = 0f
                        }
                    },
                    onTap = {
                        scope.launch {
                            draggableState.drag(MutatePriority.UserInput) {
                                // just trigger animation, press offset will be applied
                                dragBy(0f)
                            }
                            gestureEndAction.value.invoke()
                        }
                    }
                )
            }
        } else {
            this
        }
    },
    inspectorInfo = debugInspectorInfo {
        name = "sliderTapModifier"
        properties["draggableState"] = draggableState
        properties["interactionSource"] = interactionSource
        properties["maxPx"] = maxPx
        properties["isRtl"] = isRtl
        properties["rawOffset"] = rawOffset
        properties["gestureEndAction"] = gestureEndAction
        properties["pressOffset"] = pressOffset
        properties["enabled"] = enabled
    })

@Immutable
internal class CustomSliderColors internal constructor(
    private val thumbColor: Color,
    private val activeTrackColor: Color,
    private val activeTickColor: Color,
    private val inactiveTrackColor: Color,
    private val inactiveTickColor: Color,
    private val disabledThumbColor: Color,
    private val disabledActiveTrackColor: Color,
    private val disabledActiveTickColor: Color,
    private val disabledInactiveTrackColor: Color,
    private val disabledInactiveTickColor: Color
) {

    @Composable
    internal fun thumbColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) thumbColor else disabledThumbColor)
    }

    @Composable
    internal fun trackColor(enabled: Boolean, active: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (active) activeTrackColor else inactiveTrackColor
            } else {
                if (active) disabledActiveTrackColor else disabledInactiveTrackColor
            }
        )
    }

    @Composable
    internal fun tickColor(enabled: Boolean, active: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (active) activeTickColor else inactiveTickColor
            } else {
                if (active) disabledActiveTickColor else disabledInactiveTickColor
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CustomSliderColors) return false

        if (thumbColor != other.thumbColor) return false
        if (activeTrackColor != other.activeTrackColor) return false
        if (activeTickColor != other.activeTickColor) return false
        if (inactiveTrackColor != other.inactiveTrackColor) return false
        if (inactiveTickColor != other.inactiveTickColor) return false
        if (disabledThumbColor != other.disabledThumbColor) return false
        if (disabledActiveTrackColor != other.disabledActiveTrackColor) return false
        if (disabledActiveTickColor != other.disabledActiveTickColor) return false
        if (disabledInactiveTrackColor != other.disabledInactiveTrackColor) return false
        return disabledInactiveTickColor == other.disabledInactiveTickColor
    }

    override fun hashCode(): Int {
        var result = thumbColor.hashCode()
        result = 31 * result + activeTrackColor.hashCode()
        result = 31 * result + activeTickColor.hashCode()
        result = 31 * result + inactiveTrackColor.hashCode()
        result = 31 * result + inactiveTickColor.hashCode()
        result = 31 * result + disabledThumbColor.hashCode()
        result = 31 * result + disabledActiveTrackColor.hashCode()
        result = 31 * result + disabledActiveTickColor.hashCode()
        result = 31 * result + disabledInactiveTrackColor.hashCode()
        result = 31 * result + disabledInactiveTickColor.hashCode()
        return result
    }
}

// Internal to be referred to in tests
private val ThumbWidth = 20.dp
private val ThumbHeight = 20.dp
private val ThumbSize = DpSize(ThumbWidth, ThumbHeight)
private val ThumbDefaultElevation = 1.dp
private val ThumbPressedElevation = 6.dp
private val TickSize = SliderTokens.TickMarksContainerSize

// Internal to be referred to in tests
private val TrackHeight = SliderTokens.InactiveTrackHeight

private class SliderDraggableState(
    val onDelta: (Float) -> Unit
) : DraggableState {

    var isDragging by mutableStateOf(false)
        private set

    private val dragScope: DragScope = object : DragScope {
        override fun dragBy(pixels: Float): Unit = onDelta(pixels)
    }

    private val scrollMutex = MutatorMutex()

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ): Unit = coroutineScope {
        isDragging = true
        scrollMutex.mutateWith(dragScope, dragPriority, block)
        isDragging = false
    }

    override fun dispatchRawDelta(delta: Float) {
        return onDelta(delta)
    }
}

private enum class SliderComponents {
    THUMB,
    TRACK,
    SEEK_TEXT
}

/**
 * Class that holds information about [CustomSlider]'s and [RangeSlider]'s active track
 * and fractional positions where the discrete ticks should be drawn on the track.
 */
@Stable
internal class CustomSliderPositions(
    initialActiveRange: ClosedFloatingPointRange<Float> = 0f..1f,
    initialTickFractions: FloatArray = floatArrayOf()
) {
    /**
     * [ClosedFloatingPointRange] that indicates the current active range for the
     * start to thumb for a [CustomSlider] and start thumb to end thumb for a [RangeSlider].
     */
    var activeRange: ClosedFloatingPointRange<Float> by mutableStateOf(initialActiveRange)
        internal set

    /**
     * The discrete points where a tick should be drawn on the track.
     * Each value of tickFractions should be within the range [0f, 1f]. If
     * the track is continuous, then tickFractions will be an empty [FloatArray].
     */
    var tickFractions: FloatArray by mutableStateOf(initialTickFractions)
        internal set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomSliderPositions) return false

        if (activeRange != other.activeRange) return false
        return tickFractions.contentEquals(other.tickFractions)
    }

    override fun hashCode(): Int {
        var result = activeRange.hashCode()
        result = 31 * result + tickFractions.contentHashCode()
        return result
    }
}