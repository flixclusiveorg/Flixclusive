package com.flixclusive.core.ui.common.util.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

object AdaptiveUiUtil {
    val WindowWidthSizeClass.isCompact get() = this == WindowWidthSizeClass.COMPACT
    val WindowWidthSizeClass.isMedium get() = this == WindowWidthSizeClass.MEDIUM
    val WindowWidthSizeClass.isExpanded get() = this == WindowWidthSizeClass.EXPANDED

    val WindowHeightSizeClass.isCompact get() = this == WindowHeightSizeClass.COMPACT
    val WindowHeightSizeClass.isMedium get() = this == WindowHeightSizeClass.MEDIUM
    val WindowHeightSizeClass.isExpanded get() = this == WindowHeightSizeClass.EXPANDED


    /**
     * A function to get an adaptive DP size unit based on the window size class.
     *
     * @param windowSizeClass The current window size class.
     * @param compact The DP size for a compact window size. Also is the default value if no window size class is provided.
     * @param medium The DP size for a medium window size.
     * @param expanded The DP size for an expanded window size.
     *
     * @return The DP size for the current window size class.
     * */
    @Composable
    fun getAdaptiveDp(
        compact: Dp,
        medium: Dp,
        expanded: Dp,
        windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    ): Dp {
        val windowWidthSizeClass = windowSizeClass.windowWidthSizeClass
        val windowHeightSizeClass = windowSizeClass.windowHeightSizeClass

        return when {
            windowWidthSizeClass.isCompact || windowHeightSizeClass.isCompact ->  compact
            windowWidthSizeClass.isMedium || windowHeightSizeClass.isMedium -> medium
            windowWidthSizeClass.isExpanded -> expanded
            else -> compact
        }
    }


    /**
     * A function to get an adaptive TextUnit size unit based on the window size class.
     *
     * @param windowSizeClass The current window size class.
     * @param compact The TextUnit size for a compact window size. Also is the default value if no window size class is provided.
     * @param medium The TextUnit size for a medium window size.
     * @param expanded The TextUnit size for an expanded window size.
     *
     * @return The DP size for the current window size class.
     * */
    @Composable
    fun getAdaptiveTextUnit(
        compact: TextUnit,
        medium: TextUnit,
        expanded: TextUnit,
        windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    ): TextUnit {
        val windowWidthSizeClass = windowSizeClass.windowWidthSizeClass
        val windowHeightSizeClass = windowSizeClass.windowHeightSizeClass

        return when {
            windowWidthSizeClass.isCompact || windowHeightSizeClass.isCompact ->  compact
            windowWidthSizeClass.isMedium || windowHeightSizeClass.isMedium -> medium
            windowWidthSizeClass.isExpanded -> expanded
            else -> compact
        }
    }

    /**
     * A function to get an adaptive DP size unit based on the window size class.
     *
     * @param dp The DP to get an adaptive size for.
     * @param increaseBy The DP to increment the size by per window size class.
     *
     * @return The DP size for the current window size class.
     * */
    @Composable
    fun getAdaptiveDp(
        dp: Dp,
        increaseBy: Dp = 4.dp,
    ): Dp {
        return getAdaptiveDp(
            compact = dp,
            medium = dp + increaseBy,
            expanded = dp + (increaseBy * 2)
        )
    }

    /**
     * A function to get an adaptive TextUnit size unit based on the window size class.
     *
     * @param size The TextUnit to get an adaptive size for.
     * @param increaseBy The DP to increment the size by per window size class.
     *
     * @return The DP size for the current window size class.
     * */
    @Composable
    fun getAdaptiveTextUnit(
        size: TextUnit,
        increaseBy: Int = 4,
    ): TextUnit {
        return getAdaptiveTextUnit(
            compact = size,
            medium = TextUnit(
                value = size.value + increaseBy.toFloat(),
                type = size.type
            ),
            expanded = TextUnit(
                value = size.value + (increaseBy * 2).toFloat(),
                type = size.type
            )
        )
    }
}