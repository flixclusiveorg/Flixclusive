package com.flixclusive.presentation.common.composables

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flixclusive.ui.theme.lightGray

fun Modifier.placeholderEffect(
    shape: Shape = RoundedCornerShape(5.dp),
    color: Color = lightGray
)
    = graphicsLayer {
        this.shape = shape
        clip = true
    }.drawBehind {
        drawRect(color)
    }