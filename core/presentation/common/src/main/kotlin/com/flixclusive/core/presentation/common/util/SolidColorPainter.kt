package com.flixclusive.core.presentation.common.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

/**
 * A custom painter for displaying [Color]s.
 * */
internal class SolidColorPainter(private val color: Color) : Painter() {
    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRect(color = color, size = size)
    }
}
