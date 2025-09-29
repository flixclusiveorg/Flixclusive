package com.flixclusive.feature.mobile.user.add.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.mobile.extensions.fillMaxAdaptiveWidth
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp

internal object ModifierUtil {
    @Composable
    fun getHorizontalPadding() = getAdaptiveDp(16.dp)

    @Composable
    fun Modifier.fillOnBoardingContentWidth() =
        fillMaxAdaptiveWidth(
            medium = 0.8F,
            expanded = 0.9F,
        )
}
