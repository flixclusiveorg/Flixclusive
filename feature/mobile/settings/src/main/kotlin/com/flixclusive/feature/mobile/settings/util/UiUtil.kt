package com.flixclusive.feature.mobile.settings.util

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.common.util.onMediumEmphasis

internal object UiUtil {
    @Composable
    fun getEmphasizedLabel(
        size: TextUnit = 14.sp,
        letterSpacing: TextUnit = 0.1.sp
    ) = getAdaptiveTextStyle(
        style = TypographyStyle.Label,
        mode = TextStyleMode.Emphasized,
        size = size,
    ).copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = letterSpacing
    )

    @Composable
    fun getMediumEmphasizedLabel(
        size: TextUnit = 14.sp,
        emphasis: Float = 0.7F
    ) = getAdaptiveTextStyle(
        style = TypographyStyle.Label,
        mode = TextStyleMode.SemiEmphasized,
        size = size,
    ).copy(
        fontWeight = FontWeight.Black,
        color = LocalContentColor.current.onMediumEmphasis(emphasis),
    )
}