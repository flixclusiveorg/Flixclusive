package com.flixclusive.core.ui.common.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle

fun TextStyle.applyDropShadow(
    shadowColor: Color = Color.Black,
    offset: Offset = Offset(x = 2F, y = 4F),
    blurRadius: Float = 0.4F
) = this.copy(
    shadow = Shadow(
        color = shadowColor,
        offset = offset,
        blurRadius = blurRadius
    ),
)