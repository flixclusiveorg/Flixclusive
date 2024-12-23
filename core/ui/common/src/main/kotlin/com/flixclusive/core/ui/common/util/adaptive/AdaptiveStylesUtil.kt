package com.flixclusive.core.ui.common.util.adaptive

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis

enum class TypographyStyle(val size: TextUnit) {
    Body(12.sp),
    Display(24.sp),
    Headline(18.sp),
    Label(14.sp),
    Title(16.sp),
}

sealed class TextStyleMode {
    @Composable
    abstract fun getMode(style: TextStyle): TextStyle

    data object Emphasized : TextStyleMode() {
        @Composable
        override fun getMode(style: TextStyle): TextStyle {
            return style.copy(fontWeight = FontWeight.Black)
        }
    }

    data object NonEmphasized : TextStyleMode() {
        @Composable
        override fun getMode(style: TextStyle): TextStyle {
            return style.copy(
                fontWeight = FontWeight.Normal,
                color = LocalContentColor.current.onMediumEmphasis()
            )
        }
    }

    data object Normal : TextStyleMode() {
        @Composable
        override fun getMode(style: TextStyle): TextStyle {
            return style.copy(
                fontWeight = FontWeight.Normal
            )
        }
    }

    data object Light : TextStyleMode() {
        @Composable
        override fun getMode(style: TextStyle): TextStyle {
            return style.copy(
                fontWeight = FontWeight.Light
            )
        }
    }

    data object SemiEmphasized : TextStyleMode() {
        @Composable
        override fun getMode(style: TextStyle): TextStyle {
            return style.copy(
                fontWeight = FontWeight.Medium,
                color = LocalContentColor.current.onMediumEmphasis(0.8F)
            )
        }
    }
}

object AdaptiveStylesUtil {
    @Composable
    fun getAdaptiveTextStyle(
        compact: TextUnit = 12.sp,
        medium: TextUnit = (compact.value + 4).sp,
        expanded: TextUnit = (medium.value + 4).sp,
        style: TypographyStyle = TypographyStyle.Label,
        mode: TextStyleMode = TextStyleMode.NonEmphasized
    ): TextStyle {
        val materialStyle = when (style) {
            TypographyStyle.Body -> MaterialTheme.typography.bodyLarge
            TypographyStyle.Display -> MaterialTheme.typography.displayLarge
            TypographyStyle.Headline -> MaterialTheme.typography.headlineLarge
            TypographyStyle.Label -> MaterialTheme.typography.labelLarge
            TypographyStyle.Title -> MaterialTheme.typography.titleLarge
        }

        val adaptiveSize = AdaptiveUiUtil.getAdaptiveTextUnit(
            compact = compact,
            medium = medium,
            expanded = expanded
        )

        return mode
            .getMode(style = materialStyle)
            .copy(
                fontSize = adaptiveSize
            )
    }
    
    @Composable
    fun getAdaptiveTextStyle(
        style: TypographyStyle = TypographyStyle.Label,
        mode: TextStyleMode = TextStyleMode.NonEmphasized,
        size: TextUnit = style.size,
        increaseBy: TextUnit = 4.sp
    ): TextStyle {
        val materialStyle = when (style) {
            TypographyStyle.Body -> MaterialTheme.typography.bodyLarge
            TypographyStyle.Display -> MaterialTheme.typography.displayLarge
            TypographyStyle.Headline -> MaterialTheme.typography.headlineLarge
            TypographyStyle.Label -> MaterialTheme.typography.labelLarge
            TypographyStyle.Title -> MaterialTheme.typography.titleLarge
        }

        val adaptiveSize = AdaptiveUiUtil.getAdaptiveTextUnit(
            size = size,
            increaseBy = increaseBy.value
                .toInt()
        )

        return mode
            .getMode(style = materialStyle)
            .copy(
                fontSize = adaptiveSize
            )
    }
}