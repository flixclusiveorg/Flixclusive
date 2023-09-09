package com.flixclusive.presentation.common

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.flixclusive.R

val spaceGrotesk = FontFamily(
    fonts = listOf(
        Font(resId = R.font.space_grotesk_regular),
        Font(resId = R.font.space_grotesk_light, weight = FontWeight.Light),
        Font(resId = R.font.space_grotesk_medium, weight = FontWeight.Medium),
        Font(resId = R.font.space_grotesk_semibold, weight = FontWeight.SemiBold),
        Font(resId = R.font.space_grotesk_bold, weight = FontWeight.Bold)
    )
)
