package com.flixclusive.core.presentation.mobile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveTextUnit

object AdaptiveTextStyle {
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

    @Composable
    fun TextStyle.asAdaptiveTextStyle(
        size: TextUnit = this.fontSize,
        increaseBy: TextUnit = 4.sp,
    ): TextStyle {
        return asAdaptiveTextStyle(
            compact = size,
            medium = (size.value + increaseBy.value).sp,
            expanded = (size.value + (increaseBy.value * 2)).sp,
        )
    }
}

enum class TypographySize {
    Body,
    Display,
    Headline,
    Label,
    Title,
    ;

    /**
     * Returns the corresponding [TextStyle] from [MaterialTheme.typography] based on the enum value.
     * */
    internal val sizeStyle: TextStyle
        @Composable get() = when (this) {
            Body -> MaterialTheme.typography.bodyLarge
            Display -> MaterialTheme.typography.displayLarge
            Headline -> MaterialTheme.typography.headlineLarge
            Label -> MaterialTheme.typography.labelLarge
            Title -> MaterialTheme.typography.titleLarge
        }
}
