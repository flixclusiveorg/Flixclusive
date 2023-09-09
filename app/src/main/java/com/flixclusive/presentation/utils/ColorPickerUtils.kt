package com.flixclusive.presentation.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

object ColorPickerUtils {
    @Composable
    fun BoxWithColor(
        colorInt: Int,
        size: Dp = 24.dp,
    ) {
        val color = Color(colorInt)
        val boxShape = RoundedCornerShape(5)

        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    shape = boxShape
                    clip = true
                }
                .drawBehind {
                    if(colorInt == 0)
                        drawCheckeredBackground()
                    else
                        drawRect(color)
                }
                .border(
                    width = 1.dp,
                    color = contentColorFor(if(color.alpha == 0F) Color.White else color),
                    shape = boxShape
                )
        )
    }

    @Composable
    fun AlphaBar(
        modifier: Modifier = Modifier,
        color: () -> Color,
        onAlphaChanged: (Float, Color) -> Unit
    ) {
        val currentColor by rememberUpdatedState(color())

        val currentColorToAlphaBrush = remember(currentColor) {
            Brush.horizontalGradient(
                listOf(
                    currentColor.copy(alpha = 1F),
                    Color.Transparent
                )
            )
        }

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()

                        var alpha = getAlphaFromPosition(
                            x = down.position.x,
                            maxWidth = this.size.width.toFloat()
                        ).coerceIn(0F, 1F)
                        onAlphaChanged(
                            alpha,
                            currentColor.copy(alpha = alpha)
                        )

                        drag(down.id) { change ->
                            if (change.positionChange() != Offset.Zero)
                                change.consume()

                            alpha = getAlphaFromPosition(
                                x = change.position.x,
                                maxWidth = this.size.width.toFloat()
                            ).coerceIn(0F, 1F)

                            onAlphaChanged(
                                alpha,
                                currentColor.copy(alpha = alpha)
                            )
                        }
                    }
                }
        ) {
            clipRect {
                drawCheckeredBackground()
            }

            drawAlphaBar(currentColorToAlphaBrush)

            val position = getPositionFromAlpha(
                color = currentColor,
                maxWidth = this.size.width
            )

            drawHorizontalSelector(amount = position)
        }
    }

    private fun DrawScope.drawCheckeredBackground() {
        val darkColor = Color.LightGray
        val lightColor = Color.White

        val gridSizePx = 8.dp.toPx()
        val cellCountX = ceil(this.size.width / gridSizePx).toInt()
        val cellCountY = ceil(this.size.height / gridSizePx).toInt()
        for (i in 0 until cellCountX) {
            for (j in 0 until cellCountY) {
                val color = if ((i + j) % 2 == 0) darkColor else lightColor

                val x = i * gridSizePx
                val y = j * gridSizePx
                drawRect(color, Offset(x, y), Size(gridSizePx, gridSizePx))
            }
        }
    }

    private fun DrawScope.drawHorizontalSelector(amount: Float) {
        val halfIndicatorThickness = 4.dp.toPx()
        val strokeThickness = 1.dp.toPx()

        val offset =
            Offset(
                x = amount - halfIndicatorThickness,
                y = -strokeThickness
            )

        val selectionSize = Size(halfIndicatorThickness * 2f, this.size.height + strokeThickness * 2)
        drawSelectorIndicator(
            offset = offset,
            selectionSize = selectionSize,
            strokeThicknessPx = strokeThickness
        )
    }

    private fun DrawScope.drawSelectorIndicator(
        offset: Offset,
        selectionSize: Size,
        strokeThicknessPx: Float
    ) {
        val selectionStyle = Stroke(strokeThicknessPx)
        drawRect(
            Color.Gray,
            topLeft = offset,
            size = selectionSize,
            style = selectionStyle
        )
        drawRect(
            Color.White,
            topLeft = offset + Offset(strokeThicknessPx, strokeThicknessPx),
            size = selectionSize.inset(2 * strokeThicknessPx),
            style = selectionStyle
        )
    }

    private fun Size.inset(amount: Float): Size {
        return Size(width - amount, height - amount)
    }

    private fun DrawScope.drawAlphaBar(alphaBrush: Brush) {
        drawRect(alphaBrush)
        drawRect(Color.Gray, style = Stroke(0.5.dp.toPx()))
    }

    private fun getPositionFromAlpha(color: Color, maxWidth: Float): Float {
        val alpha = 1 - color.alpha
        return maxWidth * alpha
    }

    /**
     * @return new alpha calculated from the maxWidth
     */
    private fun getAlphaFromPosition(x: Float, maxWidth: Float): Float {
        return 1 - x / maxWidth
    }
}