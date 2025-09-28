package com.flixclusive.feature.mobile.settings.util

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle

@Composable
internal fun getEmphasizedLabel(
    size: TextUnit = 14.sp,
    letterSpacing: TextUnit = 0.1.sp,
) = MaterialTheme.typography.labelLarge
    .copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = letterSpacing,
    ).asAdaptiveTextStyle(size)

@Composable
internal fun getMediumEmphasizedLabel(
    size: TextUnit = 14.sp,
    emphasis: Float = 0.7F,
) = MaterialTheme.typography.labelLarge
    .copy(
        fontWeight = FontWeight.Black,
        color = LocalContentColor.current.copy(emphasis),
    ).asAdaptiveTextStyle(size)
