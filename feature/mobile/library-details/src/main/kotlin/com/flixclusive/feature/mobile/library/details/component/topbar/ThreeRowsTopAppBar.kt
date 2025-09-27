package com.flixclusive.feature.mobile.library.details.component.topbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import com.flixclusive.core.presentation.mobile.components.material3.topbar.EnterOnlyNearTopScrollBehavior
import com.flixclusive.core.presentation.mobile.components.material3.topbar.TopAppBarLayout
import com.flixclusive.core.presentation.mobile.components.material3.topbar.settleAppBar
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A two-rows top app bar that is designed to be called by the Large and Medium top app bar
 * composables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThreeRowsTopAppBar(
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    collapsedHeight: Dp,
    windowInsets: WindowInsets,
    colors: TopAppBarColors,
    scrollBehavior: EnterOnlyNearTopScrollBehavior?,
    infoContent: @Composable () -> Unit,
    filterContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    expandedHeight: Dp = TopAppBarDefaults.LargeAppBarExpandedHeight,
) {
    require(collapsedHeight.isSpecified && collapsedHeight.isFinite) {
        "The collapsedHeight is expected to be specified and finite"
    }
    require(expandedHeight.isSpecified && expandedHeight.isFinite) {
        "The expandedHeight is expected to be specified and finite"
    }
    require(expandedHeight >= collapsedHeight) {
        "The expandedHeight is expected to be greater or equal to the collapsedHeight"
    }

    val infoContentHeightPx = remember { mutableFloatStateOf(0f) }
    val filterContentHeightPx = remember { mutableFloatStateOf(0f) }
    val collapsedHeightPx: Float
    LocalDensity.current.run {
        collapsedHeightPx = collapsedHeight.toPx()
    }

    LaunchedEffect(infoContentHeightPx, filterContentHeightPx) {
        val maxHeightLimit = filterContentHeightPx.floatValue - (infoContentHeightPx.floatValue + collapsedHeightPx)
        if (scrollBehavior?.state?.heightOffsetLimit != maxHeightLimit) {
            scrollBehavior?.nearTopThreshold = abs(maxHeightLimit) - collapsedHeightPx
            scrollBehavior?.state?.heightOffsetLimit = maxHeightLimit
        }
    }

    // Obtain the container color from the TopAppBarColors using the `overlapFraction`. This
    // ensures that the colors will adjust whether the app bar behavior is pinned or scrolled.
    // This may potentially animate or interpolate a transition between the container-color and the
    // container's scrolled-color according to the app bar's scroll state.
    val colorTransitionFraction by remember(scrollBehavior) {
        // derivedStateOf to prevent redundant recompositions when the content scrolls.
        derivedStateOf {
            val overlappingFraction = scrollBehavior?.state?.overlappedFraction ?: 0f
            if (overlappingFraction > 0.01f) 1f else 0f
        }
    }
    val appBarContainerColor by animateColorAsState(
        targetValue = lerp(
            colors.containerColor,
            colors.scrolledContainerColor,
            FastOutLinearInEasing.transform(colorTransitionFraction),
        ),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    // Wrap the given actions in a Row.
    val actionsRow =
        @Composable {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }

    // Hide the top row title semantics when its alpha value goes below 0.5 threshold.
    // Hide the bottom row title semantics when the top title semantics are active.
    val hideTopRowSemantics = colorTransitionFraction < 0.5f

    // Set up support for resizing the top app bar when vertically dragging the bar itself.
    val appBarDragModifier =
        if (scrollBehavior != null && !scrollBehavior.isPinned) {
            Modifier.draggable(
                orientation = Orientation.Vertical,
                state =
                    rememberDraggableState { delta -> scrollBehavior.state.heightOffset += delta },
                onDragStopped = { velocity ->
                    settleAppBar(
                        scrollBehavior.state,
                        velocity,
                        scrollBehavior.flingAnimationSpec,
                        scrollBehavior.snapAnimationSpec,
                    )
                },
            )
        } else {
            Modifier
        }

    Surface(modifier = modifier.then(appBarDragModifier), color = appBarContainerColor) {
        Column {
            TopAppBarLayout(
                modifier =
                    Modifier
                        .windowInsetsPadding(windowInsets)
                        // clip after padding so we don't show the title over the inset area
                        .clipToBounds()
                        .heightIn(max = collapsedHeight),
                scrolledOffset = { 0f },
                navigationIconContentColor = colors.navigationIconContentColor,
                titleContentColor = colors.titleContentColor,
                actionIconContentColor = colors.actionIconContentColor,
                title = title,
                titleTextStyle = titleTextStyle,
                hideTitleSemantics = hideTopRowSemantics,
                navigationIcon = navigationIcon,
                actions = actionsRow,
            )

            Layout(
                content = infoContent,
                modifier = Modifier
                    // only apply the horizontal sides of the window insets padding, since the
                    // top
                    // padding will always be applied by the layout above
                    .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal))
                    .clipToBounds(),
            ) { measurables, constraints ->
                // Measure children first to get their actual height
                val placeables = measurables.map { it.measure(constraints.copy(maxHeight = Constraints.Infinity)) }

                // Calculate the actual content height
                val actualContentHeight = placeables.sumOf { it.height }

                infoContentHeightPx.floatValue = actualContentHeight.toFloat()

                // Subtract the scrolledOffset from the height. The scrolledOffset is expected to be
                // equal or smaller than zero.
                val scrolledOffsetValue = scrollBehavior?.state?.heightOffset ?: 0f
                val heightOffset = if (scrolledOffsetValue.isNaN()) 0 else scrolledOffsetValue.roundToInt()

                val layoutHeight = (actualContentHeight + heightOffset).coerceAtLeast(0)

                layout(constraints.maxWidth, layoutHeight) {
                    placeables.forEachIndexed { index, placeable ->
                        placeable.place(0, index * placeable.height)
                    }
                }
            }

            Layout(
                content = filterContent,
                modifier = Modifier
                    // only apply the horizontal sides of the window insets padding, since the
                    // top
                    // padding will always be applied by the layout above
                    .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal))
                    .clipToBounds(),
            ) { measurables, constraints ->
                // Measure children first to get their actual height
                val placeables = measurables.map { it.measure(constraints.copy(maxHeight = Constraints.Infinity)) }

                // Calculate the actual content height
                val actualContentHeight = placeables.sumOf { it.height }

                filterContentHeightPx.floatValue = actualContentHeight.toFloat()

                layout(constraints.maxWidth, actualContentHeight) {
                    placeables.forEachIndexed { index, placeable ->
                        placeable.place(0, index * placeable.height)
                    }
                }
            }
        }
    }
}

internal val TopTitleAlphaEasing = CubicBezierEasing(.8f, 0f, .8f, .15f)
