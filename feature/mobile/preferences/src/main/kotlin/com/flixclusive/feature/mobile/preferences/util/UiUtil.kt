package com.flixclusive.feature.mobile.preferences.util

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.onMediumEmphasis

internal object UiUtil {
    @Composable
    fun getEmphasizedLabel(
        size: TextUnit = 14.sp,
        letterSpacing: TextUnit = 0.1.sp
    ) = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = letterSpacing,
        fontSize = size
    )

    @Composable
    fun getMediumEmphasizedLabel(
        size: TextUnit = 14.sp,
        emphasis: Float = 0.7F
    ) = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Black,
        color = LocalContentColor.current.onMediumEmphasis(emphasis),
        fontSize = size
    )

    @Composable
    fun getNonEmphasizedLabel(size: TextUnit = 14.sp)
        = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Normal,
            color = LocalContentColor.current.onMediumEmphasis(),
            fontSize = size
        )
}