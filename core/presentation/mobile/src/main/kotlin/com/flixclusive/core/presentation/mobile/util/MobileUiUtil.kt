package com.flixclusive.core.presentation.mobile.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp

/**
 * Utility object for mobile UI components, providing default dimensions
 * and adaptive sizing functions to enhance the user experience across
 * different screen sizes.
 * */
object MobileUiUtil {
    val DefaultScreenPaddingHorizontal = 8.dp
    val DefaultFilmCardPosterWidth = 110.dp

    /**
     * Returns an adaptive width for film cards based on the screen size.
     * On larger screens, the width is increased by [increasedBy] Dp to
     * provide a better visual experience.
     * */
    @Composable
    fun getAdaptiveFilmCardWidth(
        dp: Dp = DefaultFilmCardPosterWidth,
        increasedBy: Dp = 75.dp,
    ): Dp {
        return getAdaptiveDp(
            dp = dp,
            increaseBy = increasedBy
        )
    }
}
