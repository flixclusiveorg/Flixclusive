package com.flixclusive.core.presentation.mobile.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveTextUnit

object AdaptiveTextStyle {
    @Stable
    @Composable
    fun TextStyle.asAdaptiveTextStyle(
        compact: TextUnit,
        medium: TextUnit = (compact.value + 4).sp,
        expanded: TextUnit = (medium.value + 4).sp,
    ): TextStyle {
        val adaptiveSize = getAdaptiveTextUnit(
            compact = compact,
            medium = medium,
            expanded = expanded,
        )

        return this.copy(fontSize = adaptiveSize)
    }

    @Stable
    @Composable
    fun TextStyle.asAdaptiveTextStyle(
        size: TextUnit = this.fontSize,
        increaseBy: TextUnit = 2.sp,
    ): TextStyle {
        return asAdaptiveTextStyle(
            compact = size,
            medium = (size.value + increaseBy.value).sp,
            expanded = (size.value + (increaseBy.value * 2)).sp,
        )
    }
}
