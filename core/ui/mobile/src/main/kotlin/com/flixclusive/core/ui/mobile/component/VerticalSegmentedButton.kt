package com.flixclusive.core.ui.mobile.component

/*
*
* Copied from SegmentedButtons.kt of Compose Material3
*
* */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis

@Composable
@ReadOnlyComposable
fun getVerticalSegmentedShape(
    index: Int,
    count: Int,
    baseShape: CornerBasedShape = MaterialTheme.shapes.small
): Shape {
    if (count == 1) {
        return baseShape
    }

    return when (index) {
        0 -> baseShape.top()
        count - 1 -> baseShape.bottom()
        else -> RectangleShape
    }
}

private fun CornerBasedShape.bottom(): CornerBasedShape {
    return copy(
        topEnd = CornerSize(0.0.dp),
        topStart = CornerSize(0.0.dp)
    )
}

private fun CornerBasedShape.top(): CornerBasedShape {
    return copy(
        bottomEnd = CornerSize(0.0.dp),
        bottomStart = CornerSize(0.0.dp)
    )
}

@Composable
internal fun getDefaultSegmentedButtonColors() = SegmentedButtonDefaults.colors().copy(
    activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05F),
    activeContentColor = MaterialTheme.colorScheme.primary,
    inactiveContentColor = MaterialTheme.colorScheme.onSurface.onMediumEmphasis(),
    disabledActiveContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05F),
    disabledActiveContentColor = MaterialTheme.colorScheme.primary,
    disabledInactiveContentColor = MaterialTheme.colorScheme.onSurface.onMediumEmphasis()
)

@Composable
@ExperimentalMaterial3Api
fun VerticalSegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = getDefaultSegmentedButtonColors(),
    border: BorderStroke = SegmentedButtonDefaults.borderStroke(
        color = colors.activeBorderColor.copy(alpha = 0.5F),
        width = 1.2.dp
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    icon: @Composable () -> Unit = { SegmentedButtonDefaults.Icon(selected) },
    label: @Composable () -> Unit,
) {
    val containerColor = colors.containerColor(enabled, selected)
    val contentColor = colors.contentColor(enabled, selected)
    val interactionCount = interactionSource.interactionCountAsState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .interactionZIndex(selected, interactionCount)
            .semantics { role = Role.RadioButton },
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border,
        interactionSource = interactionSource
    ) {
        SegmentedButtonContent(icon, label)
    }
}

@Composable
@ExperimentalMaterial3Api
fun SingleChoiceSegmentedButtonColumn(
    modifier: Modifier = Modifier,
    space: Dp = SegmentedButtonDefaults.BorderWidth,
    content: @Composable SingleChoiceSegmentedButtonColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .selectableGroup()
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(-space),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val scope = remember { SingleChoiceSegmentedButtonScopeWrapper(this) }
        scope.content()
    }
}

@ExperimentalMaterial3Api
@Composable
private fun SegmentedButtonContent(
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    ProvideTextStyle(
        value = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(ButtonDefaults.TextButtonContentPadding)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                content()
            }

            icon()
        }
    }
}

@Composable
private fun InteractionSource.interactionCountAsState(): State<Int> {
    val interactionCount = remember { mutableIntStateOf(0) }
    LaunchedEffect(this) {
        this@interactionCountAsState.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press,
                is FocusInteraction.Focus -> {
                    interactionCount.intValue++
                }

                is PressInteraction.Release,
                is FocusInteraction.Unfocus,
                is PressInteraction.Cancel -> {
                    interactionCount.intValue--
                }
            }
        }
    }

    return interactionCount
}

/**
 * Represents the content color passed to the items
 *
 * @param enabled whether the [VerticalSegmentedButton] is enabled or not
 * @param checked whether the [VerticalSegmentedButton] item is checked or not
 */
@Stable
@OptIn(ExperimentalMaterial3Api::class)
private fun SegmentedButtonColors.contentColor(
    enabled: Boolean,
    checked: Boolean
): Color {
    return when {
        enabled && checked -> activeContentColor
        enabled && !checked -> inactiveContentColor
        !enabled && checked -> disabledActiveContentColor
        else -> disabledInactiveContentColor
    }
}

/**
 * Represents the container color passed to the items
 *
 * @param enabled whether the [VerticalSegmentedButton] is enabled or not
 * @param active whether the [VerticalSegmentedButton] item is active or not
 */
@Stable
@OptIn(ExperimentalMaterial3Api::class)
private fun SegmentedButtonColors.containerColor(
    enabled: Boolean,
    active: Boolean
): Color {
    return when {
        enabled && active -> activeContainerColor
        enabled && !active -> inactiveContainerColor
        !enabled && active -> disabledActiveContainerColor
        else -> disabledInactiveContainerColor
    }
}

/** Scope for the children of a [SingleChoiceSegmentedButtonColumn] */
@ExperimentalMaterial3Api
interface SingleChoiceSegmentedButtonColumnScope : ColumnScope

private fun Modifier.interactionZIndex(
    checked: Boolean,
    interactionCount: State<Int>
) = this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val zIndex = interactionCount.value + if (checked) CheckedZIndexFactor else 0f
            placeable.place(0, 0, zIndex)
        }
    }

private const val CheckedZIndexFactor = 5f

@OptIn(ExperimentalMaterial3Api::class)
private class SingleChoiceSegmentedButtonScopeWrapper(scope: ColumnScope) :
    SingleChoiceSegmentedButtonColumnScope, ColumnScope by scope
