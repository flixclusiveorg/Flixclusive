package com.flixclusive.feature.mobile.user.add.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveModifierUtil.fillMaxAdaptiveWidth
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp

internal object ModifierUtil {
    @Composable
    fun getHorizontalPadding() = getAdaptiveDp(16.dp)

    fun Modifier.fillOnBoardingContentWidth()
        = fillMaxAdaptiveWidth(
            medium = 0.8F,
            expanded = 0.9F
        )
}