package com.flixclusive.core.ui.common.util.adaptive

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis

object AdaptiveStylesUtil {
    @Composable
    fun getAdaptiveNonEmphasizedLabel(
        compactFontSize: TextUnit = 14.sp,
        mediumFontSize: TextUnit = (compactFontSize.value + 4).sp,
        expandedFontSize: TextUnit = (mediumFontSize.value + 4).sp
    ): TextStyle {
        return MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Normal,
            color = LocalContentColor.current.onMediumEmphasis(),
            fontSize = AdaptiveUiUtil.getAdaptiveTextUnit(
                compact = compactFontSize,
                medium = mediumFontSize,
                expanded = expandedFontSize
            )
        )
    }

    @Composable
    fun getAdaptiveSemiEmphasizedLabel(
        compactFontSize: TextUnit = 14.sp,
        mediumFontSize: TextUnit = (compactFontSize.value + 4).sp,
        expandedFontSize: TextUnit = (mediumFontSize.value + 4).sp
    ): TextStyle {
        return MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Medium,
            color = LocalContentColor.current.onMediumEmphasis(0.8F),
            fontSize = AdaptiveUiUtil.getAdaptiveTextUnit(
                compact = compactFontSize,
                medium = mediumFontSize,
                expanded = expandedFontSize
            )
        )
    }

    @Composable
    fun getAdaptiveEmphasizedLabel(
        compactFontSize: TextUnit = 14.sp,
        mediumFontSize: TextUnit = (compactFontSize.value + 4).sp,
        expandedFontSize: TextUnit = (mediumFontSize.value + 4).sp
    ): TextStyle {
        return MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Black,
            color = LocalContentColor.current,
            fontSize = AdaptiveUiUtil.getAdaptiveTextUnit(
                compact = compactFontSize,
                medium = mediumFontSize,
                expanded = expandedFontSize
            )
        )
    }
}