package com.flixclusive.core.presentation.mobile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import kotlin.math.floor
import kotlin.math.max

// TODO: Find all labeled checkbox and replace them with this.

/**
 * A checkbox with a label next to it.
 *
 * @param checked whether the checkbox is checked or not
 * @param onCheckedChange callback that is triggered when the checkbox is checked or unchecked
 * @param modifier the [Modifier] to be applied to this checkbox
 * @param enabled whether the checkbox is enabled or not
 * @param checkboxSize the size of the checkbox
 * @param colors the [CheckboxColors] to be used for this checkbox
 * @param interactionSource the [MutableInteractionSource] to be used for this checkbox
 * @param label the label to be displayed next to the checkbox
 * */
@Composable
fun LabeledCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkboxSize: Dp = getAdaptiveDp(CheckboxSize),
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    label: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CustomCheckbox(
            enabled = enabled,
            checked = checked,
            onCheckedChange = onCheckedChange,
            interactionSource = interactionSource,
            colors = colors.copy(
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.6f),
            ),
            modifier = Modifier.size(checkboxSize),
        )

        label()
    }
}

@Composable
fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    CustomTriStateCheckbox(
        state = ToggleableState(checked),
        onClick = if (onCheckedChange != null) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}

@Composable
fun CustomTriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val toggleableModifier =
        if (onClick != null) {
            @Suppress("DEPRECATION_ERROR")
            Modifier.triStateToggleable(
                state = state,
                onClick = onClick,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = 40.dp / 2,
                ),
            )
        } else {
            Modifier
        }
    CheckboxImpl(
        enabled = enabled,
        value = state,
        modifier = modifier
            .then(toggleableModifier),
        colors = colors,
    )
}

@Composable
private fun CheckboxImpl(
    enabled: Boolean,
    value: ToggleableState,
    modifier: Modifier = Modifier,
    colors: CheckboxColors,
) {
    val transition = updateTransition(value, label = "")
    val checkDrawFraction = transition.animateFloat(
        transitionSpec = {
            when {
                initialState == ToggleableState.Off -> tween(CHECK_ANIMATION_DURATION)
                targetState == ToggleableState.Off -> snap(BOX_OUT_DURATION)
                else -> spring()
            }
        },
        label = "",
    ) {
        when (it) {
            ToggleableState.On -> 1f
            ToggleableState.Off -> 0f
            ToggleableState.Indeterminate -> 1f
        }
    }

    val checkCenterGravitationShiftFraction = transition.animateFloat(
        transitionSpec = {
            when {
                initialState == ToggleableState.Off -> snap()
                targetState == ToggleableState.Off -> snap(BOX_OUT_DURATION)
                else -> tween(durationMillis = CHECK_ANIMATION_DURATION)
            }
        },
        label = "",
    ) {
        when (it) {
            ToggleableState.On -> 0f
            ToggleableState.Off -> 0f
            ToggleableState.Indeterminate -> 1f
        }
    }
    val checkCache = remember { CheckDrawingCache() }
    val checkColor = colors.checkmarkColor(value)
    val boxColor = colors.boxColor(enabled, value)
    val borderColor = colors.borderColor(enabled, value)
    Canvas(
        modifier
            .wrapContentSize(Alignment.Center)
            .requiredSize(CheckboxSize),
    ) {
        val strokeWidthPx = floor(StrokeWidth.toPx())
        drawBox(
            boxColor = boxColor.value,
            borderColor = borderColor.value,
            radius = RadiusSize.toPx(),
            strokeWidth = strokeWidthPx,
        )
        drawCheck(
            checkColor = checkColor.value,
            checkFraction = checkDrawFraction.value,
            crossCenterGravitation = checkCenterGravitationShiftFraction.value,
            strokeWidthPx = strokeWidthPx,
            drawingCache = checkCache,
        )
    }
}

private fun DrawScope.drawBox(
    boxColor: Color,
    borderColor: Color,
    radius: Float,
    strokeWidth: Float,
) {
    val halfStrokeWidth = strokeWidth / 2.0f
    val stroke = Stroke(strokeWidth)
    val checkboxSize = size.width
    if (boxColor == borderColor) {
        drawRoundRect(
            boxColor,
            size = Size(checkboxSize, checkboxSize),
            cornerRadius = CornerRadius(radius),
            style = Fill,
        )
    } else {
        drawRoundRect(
            boxColor,
            topLeft = Offset(strokeWidth, strokeWidth),
            size = Size(checkboxSize - strokeWidth * 2, checkboxSize - strokeWidth * 2),
            cornerRadius = CornerRadius(max(0f, radius - strokeWidth)),
            style = Fill,
        )
        drawRoundRect(
            borderColor,
            topLeft = Offset(halfStrokeWidth, halfStrokeWidth),
            size = Size(checkboxSize - strokeWidth, checkboxSize - strokeWidth),
            cornerRadius = CornerRadius(radius - halfStrokeWidth),
            style = stroke,
        )
    }
}

private fun DrawScope.drawCheck(
    checkColor: Color,
    checkFraction: Float,
    crossCenterGravitation: Float,
    strokeWidthPx: Float,
    drawingCache: CheckDrawingCache,
) {
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Square)
    val width = size.width
    val checkCrossX = 0.4f
    val checkCrossY = 0.7f
    val leftX = 0.2f
    val leftY = 0.5f
    val rightX = 0.8f
    val rightY = 0.3f

    val gravitatedCrossX = lerp(checkCrossX, 0.5f, crossCenterGravitation)
    val gravitatedCrossY = lerp(checkCrossY, 0.5f, crossCenterGravitation)
    // gravitate only Y for end to achieve center line
    val gravitatedLeftY = lerp(leftY, 0.5f, crossCenterGravitation)
    val gravitatedRightY = lerp(rightY, 0.5f, crossCenterGravitation)

    with(drawingCache) {
        checkPath.reset()
        checkPath.moveTo(width * leftX, width * gravitatedLeftY)
        checkPath.lineTo(width * gravitatedCrossX, width * gravitatedCrossY)
        checkPath.lineTo(width * rightX, width * gravitatedRightY)
        // TO-DO: replace with proper declarative non-android alternative when ready (b/158188351)
        pathMeasure.setPath(checkPath, false)
        pathToDraw.reset()
        pathMeasure.getSegment(
            0f,
            pathMeasure.length * checkFraction,
            pathToDraw,
            true,
        )
    }
    drawPath(drawingCache.pathToDraw, checkColor, style = stroke)
}

@Immutable
private class CheckDrawingCache(
    val checkPath: Path = Path(),
    val pathMeasure: PathMeasure = PathMeasure(),
    val pathToDraw: Path = Path(),
)

@Composable
internal fun CheckboxColors.checkmarkColor(state: ToggleableState): State<Color> {
    val target = if (state == ToggleableState.Off) {
        uncheckedCheckmarkColor
    } else {
        checkedCheckmarkColor
    }

    val duration = if (state == ToggleableState.Off) BOX_OUT_DURATION else BOX_IN_DURATION
    return animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = duration),
        label = "",
    )
}

/**
 * Represents the color used for the box (background) of the checkbox, depending on [enabled]
 * and [state].
 *
 * @param enabled whether the checkbox is enabled or not
 * @param state the [ToggleableState] of the checkbox
 */
@Composable
internal fun CheckboxColors.boxColor(
    enabled: Boolean,
    state: ToggleableState,
): State<Color> {
    val target = if (enabled) {
        when (state) {
            ToggleableState.On, ToggleableState.Indeterminate -> checkedBoxColor
            ToggleableState.Off -> uncheckedBoxColor
        }
    } else {
        when (state) {
            ToggleableState.On -> disabledCheckedBoxColor
            ToggleableState.Indeterminate -> disabledIndeterminateBoxColor
            ToggleableState.Off -> disabledUncheckedBoxColor
        }
    }

    // If not enabled 'snap' to the disabled state, as there should be no animations between
    // enabled / disabled.
    return if (enabled) {
        val duration = if (state == ToggleableState.Off) BOX_OUT_DURATION else BOX_IN_DURATION
        animateColorAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = duration),
            label = "",
        )
    } else {
        rememberUpdatedState(target)
    }
}

/**
 * Represents the color used for the border of the checkbox, depending on [enabled] and [state].
 *
 * @param enabled whether the checkbox is enabled or not
 * @param state the [ToggleableState] of the checkbox
 */
@Composable
internal fun CheckboxColors.borderColor(
    enabled: Boolean,
    state: ToggleableState,
): State<Color> {
    val target = if (enabled) {
        when (state) {
            ToggleableState.On, ToggleableState.Indeterminate -> checkedBorderColor
            ToggleableState.Off -> uncheckedBorderColor
        }
    } else {
        when (state) {
            ToggleableState.Indeterminate -> disabledIndeterminateBorderColor
            ToggleableState.On -> disabledBorderColor
            ToggleableState.Off -> disabledUncheckedBorderColor
        }
    }

    // If not enabled 'snap' to the disabled state, as there should be no animations between
    // enabled / disabled.
    return if (enabled) {
        val duration = if (state == ToggleableState.Off) BOX_OUT_DURATION else BOX_IN_DURATION
        animateColorAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = duration),
            label = "",
        )
    } else {
        rememberUpdatedState(target)
    }
}

private const val BOX_IN_DURATION = 50
private const val BOX_OUT_DURATION = 100
private const val CHECK_ANIMATION_DURATION = 100

private val CheckboxSize = 18.dp
private val StrokeWidth = 2.dp
private val RadiusSize = 2.dp
