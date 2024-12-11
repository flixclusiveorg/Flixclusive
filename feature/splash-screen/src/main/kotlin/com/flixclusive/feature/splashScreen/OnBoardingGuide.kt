package com.flixclusive.feature.splashScreen

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter

@Stable
internal interface OnBoardingGuide {
    val title: String
    val description: String
    val image: Painter?
        get() = null
}