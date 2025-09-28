package com.flixclusive.feature.mobile.search.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp

internal object SearchUiUtils {
    @Stable
    @Composable
    fun getCardWidth(base: Dp = 145.dp) = getAdaptiveDp(base, 50.dp)
}
