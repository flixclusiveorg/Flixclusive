package com.flixclusive.core.presentation.mobile

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveTextUnit

sealed class AdaptiveTextStyle(
    val size: TypographySize,
) {
    open val textStyle: TextStyle
        @Composable get() = size.sizeStyle

    class Emphasized(
        size: TypographySize = TypographySize.Label,
    ) : AdaptiveTextStyle(size) {
        override val textStyle: TextStyle
            @Composable get() = super.textStyle
                .copy(fontWeight = FontWeight.Black)
    }

    class NonEmphasized(
        size: TypographySize = TypographySize.Label,
    ) : AdaptiveTextStyle(size) {
        override val textStyle: TextStyle
            @Composable get() = super.textStyle
                .copy(
                    fontWeight = FontWeight.Normal,
                    color = LocalContentColor.current.copy(0.6F),
                )
    }

    class Normal(
        size: TypographySize = TypographySize.Label,
    ) : AdaptiveTextStyle(size) {
        override val textStyle: TextStyle
            @Composable get() = super.textStyle
                .copy(fontWeight = FontWeight.Normal)
    }

    class Light(
        size: TypographySize = TypographySize.Label,
    ) : AdaptiveTextStyle(size) {
        override val textStyle: TextStyle
            @Composable get() = super.textStyle
                .copy(fontWeight = FontWeight.Light)
    }

    class SemiEmphasized(
        size: TypographySize = TypographySize.Label,
    ) : AdaptiveTextStyle(size) {
        override val textStyle: TextStyle
            @Composable get() = super.textStyle
                .copy(
                    fontWeight = FontWeight.Medium,
                    color = LocalContentColor.current.copy(alpha = 0.8F),
                )
    }

    companion object {
        @Composable
        fun getAdaptiveTextStyle(
            compact: TextUnit = 12.sp,
            medium: TextUnit = (compact.value + 4).sp,
            expanded: TextUnit = (medium.value + 4).sp,
            style: AdaptiveTextStyle = Normal(),
        ): TextStyle {
            val adaptiveSize = getAdaptiveTextUnit(
                compact = compact,
                medium = medium,
                expanded = expanded,
            )

            return style.textStyle.copy(fontSize = adaptiveSize)
        }

        @Composable
        fun getAdaptiveTextStyle(
            style: AdaptiveTextStyle = Normal(),
            size: TextUnit = style.textStyle.fontSize,
            increaseBy: TextUnit = 4.sp,
        ): TextStyle {
            val adaptiveSize = getAdaptiveTextUnit(
                size = size,
                increaseBy = increaseBy.value.toInt(),
            )

            return style.textStyle.copy(fontSize = adaptiveSize)
        }
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
