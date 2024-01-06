package com.flixclusive.core.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

internal val spaceGrotesk = FontFamily(
    fonts = listOf(
        Font(resId = R.font.space_grotesk_regular),
        Font(resId = R.font.space_grotesk_light, weight = FontWeight.Light),
        Font(resId = R.font.space_grotesk_medium, weight = FontWeight.Medium),
        Font(resId = R.font.space_grotesk_semibold, weight = FontWeight.SemiBold),
        Font(resId = R.font.space_grotesk_bold, weight = FontWeight.Bold)
    )
)
