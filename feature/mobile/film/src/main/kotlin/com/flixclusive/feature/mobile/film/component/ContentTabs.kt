package com.flixclusive.feature.mobile.film.component

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.feature.mobile.film.ContentTabType

/**
 * Custom implementation of a TabRow with adaptive width tabs.
 * */
@Composable
internal fun ContentTabs(
    tabs: List<ContentTabType>,
    currentTabSelected: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minTabWidth: Dp = 80.dp,
) {
    val containerColor: Color = TabRowDefaults.primaryContainerColor
    val contentColor: Color = TabRowDefaults.primaryContentColor

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val fillMaxWidth = windowSizeClass.windowWidthSizeClass.isCompact

    Surface(
        modifier = modifier
            .selectableGroup(),
        color = containerColor,
        contentColor = contentColor,
    ) {
        SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
            // First, measure tabs with unconstrained width to get their preferred sizes
            val tabMeasurables = subcompose("Tabs") {
                Tabs(
                    tabTypes = tabs,
                    currentTabSelected = currentTabSelected,
                    onTabChange = onTabChange,
                )
            }

            val tabCount = tabMeasurables.size

            // Calculate the height of each tab
            val tabRowWidth = constraints.maxWidth
            val individualTabWidth = when {
                tabCount > 0 -> tabRowWidth / tabCount
                else -> 0
            }
            val tabRowHeight =
                tabMeasurables.fastFold(initial = 0) { max, curr ->
                    maxOf(curr.maxIntrinsicHeight(individualTabWidth), max)
                }

            // Calculate total width of all tabs at their intrinsic sizes
            val totalTabsWidth = tabMeasurables.sumOf {
                it.maxIntrinsicWidth(Int.MAX_VALUE)
            }

            // Re-measure tabs with proper constraints if they need to be constrained
            val tabMaxWidth = when {
                fillMaxWidth || totalTabsWidth > tabRowWidth -> individualTabWidth
                else -> Constraints.Infinity
            }
            val tabPlaceables = tabMeasurables.fastMap {
                it.measure(
                    constraints.copy(
                        minWidth = minTabWidth.roundToPx(),
                        maxWidth = tabMaxWidth,
                        minHeight = tabRowHeight,
                        maxHeight = tabRowHeight,
                    ),
                )
            }

            // Calculate tab positions based on actual widths
            val tabPositions = mutableListOf<TabPosition>()

            tabPlaceables.fastFold(initial = 0.dp) { total, placeable ->
                val currentWidth = placeable.width.toDp()
                tabPositions.add(
                    TabPosition(
                        left = total,
                        width = currentWidth,
                    ),
                )

                total + currentWidth
            }

            layout(constraints.maxWidth, tabRowHeight) {
                // Place tabs at their calculated positions
                var xOffset = 0
                tabPlaceables.fastForEach { placeable ->
                    placeable.placeRelative(xOffset, 0)
                    xOffset += placeable.width
                }

                subcompose("Divider") {
                    HorizontalDivider(
                        thickness = getAdaptiveDp(0.5.dp, 0.8.dp),
                        color = LocalContentColor.current.copy(0.2F),
                    )
                }.fastForEach {
                    val placeable = it.measure(constraints.copy(minHeight = 0))
                    placeable.placeRelative(0, tabRowHeight - placeable.height)
                }

                subcompose("Indicator") {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[currentTabSelected])
                            .padding(horizontal = 25.dp),
                        width = Dp.Unspecified,
                    )
                }.fastForEach {
                    // Use the actual tab width for the indicator
                    val indicatorWidth = tabPositions[currentTabSelected].width.roundToPx()
                    it.measure(Constraints.fixed(indicatorWidth, tabRowHeight)).placeRelative(0, 0)
                }
            }
        }
    }
}

@Composable
private fun Tabs(
    tabTypes: List<ContentTabType>,
    currentTabSelected: Int,
    onTabChange: (Int) -> Unit,
) {
    tabTypes.forEachIndexed { index, filmTab ->
        val isSelected = currentTabSelected == index

        Tab(
            text = {
                Text(
                    text = stringResource(id = filmTab.stringId),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.asAdaptiveTextStyle(),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            },
            selected = isSelected,
            onClick = { onTabChange(index) },
        )
    }
}

@Preview
@Composable
private fun ContentTabsBasePreview() {
    var selectedTabType by remember { mutableIntStateOf(0) }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            ContentTabs(
                tabs = ContentTabType.entries,
                currentTabSelected = selectedTabType,
                onTabChange = { selectedTabType = it },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ContentTabsCompactLandscapePreview() {
    ContentTabsBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ContentTabsMediumPortraitPreview() {
    ContentTabsBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ContentTabsMediumLandscapePreview() {
    ContentTabsBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ContentTabsExtendedPortraitPreview() {
    ContentTabsBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ContentTabsExtendedLandscapePreview() {
    ContentTabsBasePreview()
}

/**
 * [Modifier] that takes up all the available width inside the [TabRow], and then animates the
 * offset of the indicator it is applied to, depending on the [currentTabPosition].
 *
 * @param currentTabPosition [TabPosition] of the currently selected tab. This is used to
 *   calculate the offset of the indicator this modifier is applied to, as well as its width.
 */
@Suppress("ktlint:compose:modifier-composed-check")
private fun Modifier.tabIndicatorOffset(currentTabPosition: TabPosition): Modifier =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "tabIndicatorOffset"
                value = currentTabPosition
            },
    ) {
        val currentTabWidth by
            animateDpAsState(
                targetValue = currentTabPosition.width,
                animationSpec = TabRowIndicatorSpec,
            )
        val indicatorOffset by
            animateDpAsState(
                targetValue = currentTabPosition.left,
                animationSpec = TabRowIndicatorSpec,
            )
        fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset { IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
            .width(currentTabWidth)
    }

/** [AnimationSpec] used when an indicator is updating width and/or offset. */
private val TabRowIndicatorSpec: AnimationSpec<Dp> =
    tween(durationMillis = 250, easing = FastOutSlowInEasing)

@Immutable
internal class TabPosition(
    val left: Dp,
    val width: Dp,
) {
    val right: Dp
        get() = left + width

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TabPosition) return false

        if (left != other.left) return false
        if (width != other.width) return false

        return true
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + width.hashCode()
        return result
    }

    override fun toString(): String {
        return "TabPosition(left=$left, right=$right, width=$width)"
    }
}
