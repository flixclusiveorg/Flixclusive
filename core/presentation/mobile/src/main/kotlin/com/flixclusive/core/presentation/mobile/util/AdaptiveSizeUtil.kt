package com.flixclusive.core.presentation.mobile.util

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.extensions.isExpanded
import com.flixclusive.core.presentation.mobile.extensions.isMedium

object AdaptiveSizeUtil {
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
    @Stable
    fun getAdaptiveDp(
        compact: Dp,
        medium: Dp,
        expanded: Dp,
        windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
    ): Dp {
        val windowWidthSizeClass = windowSizeClass.windowWidthSizeClass
        val windowHeightSizeClass = windowSizeClass.windowHeightSizeClass

        return when {
            windowWidthSizeClass.isCompact || windowHeightSizeClass.isCompact -> compact
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
    @Stable
    fun getAdaptiveTextUnit(
        compact: TextUnit,
        medium: TextUnit,
        expanded: TextUnit,
        windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
    ): TextUnit {
        val windowWidthSizeClass = windowSizeClass.windowWidthSizeClass
        val windowHeightSizeClass = windowSizeClass.windowHeightSizeClass

        return when {
            windowWidthSizeClass.isCompact || windowHeightSizeClass.isCompact -> compact
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
    @Stable
    fun getAdaptiveDp(
        dp: Dp,
        increaseBy: Dp = 4.dp,
    ): Dp {
        return getAdaptiveDp(
            compact = dp,
            medium = dp + increaseBy,
            expanded = dp + (increaseBy * 2),
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
    @Stable
    fun getAdaptiveTextUnit(
        size: TextUnit,
        increaseBy: Int = 4,
    ): TextUnit {
        return getAdaptiveTextUnit(
            compact = size,
            medium = TextUnit(
                value = size.value + increaseBy.toFloat(),
                type = size.type,
            ),
            expanded = TextUnit(
                value = size.value + (increaseBy * 2).toFloat(),
                type = size.type,
            ),
        )
    }

    /**
     * A function to get an adaptive number of grid cells based on the window size class.
     *
     * @param compact The number of grid cells for a compact window size. Default is 1.
     * @param medium The number of grid cells for a medium window size. Default is 2.
     * @param expanded The number of grid cells for an expanded window size. Default is 3.
     *
     * @return A [GridCells.Fixed] with the number of cells for the current window size class.
     * */
    @Composable
    @Stable
    fun getAdaptiveGridCellsCount(
        compact: Int = 1,
        medium: Int = 2,
        expanded: Int = 3,
    ): GridCells.Fixed {
        val windowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

        val columns = when {
            windowWidthSizeClass.isCompact -> compact
            windowWidthSizeClass.isMedium -> medium
            windowWidthSizeClass.isExpanded -> expanded
            else -> compact
        }

        return GridCells.Fixed(columns)
    }
}
