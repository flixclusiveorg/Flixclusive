package com.flixclusive.core.ui.common.util

import android.graphics.BlurMaskFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.flixclusive.core.theme.lightGray


fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

fun Modifier.placeholderEffect(
    shape: Shape = RoundedCornerShape(5.dp),
    color: Color = lightGray,
) = graphicsLayer {
    this.shape = shape
    clip = true
}.drawBehind {
    drawRect(color)
}

/**
 * Used to apply modifiers conditionally.
 */
internal fun Modifier.ifElse(
    condition: () -> Boolean,
    ifTrueModifier: Modifier,
    ifFalseModifier: Modifier = Modifier
): Modifier = then(if (condition()) ifTrueModifier else ifFalseModifier)

/**
 * Used to apply modifiers conditionally.
 */
fun Modifier.ifElse(
    condition: Boolean,
    ifTrueModifier: Modifier,
    ifFalseModifier: Modifier = Modifier
): Modifier = ifElse({ condition }, ifTrueModifier, ifFalseModifier)

fun Modifier.noIndicationClickable(onClick: () -> Unit) = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick,
    )
}

/**
 * Can be used for neumorphism
 * */
fun Modifier.dropShadow(
    color: Color = Color.Black,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    blurRadius: Dp = 0.dp,
) = then(
    drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            if (blurRadius != 0.dp) {
                frameworkPaint.maskFilter = (BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL))
            }
            frameworkPaint.color = color.toArgb()

            val leftPixel = offsetX.toPx()
            val topPixel = offsetY.toPx()
            val rightPixel = size.width + topPixel
            val bottomPixel = size.height + leftPixel

            canvas.drawRect(
                left = leftPixel,
                top = topPixel,
                right = rightPixel,
                bottom = bottomPixel,
                paint = paint,
            )
        }
    }
)

/**
 * Applies a shadow to the current box.
 *
 * @param color The color of the shadow.
 *
 * @param blurRadius The larger this value, the bigger the blur, so the shadow
 * becomes bigger and lighter.
 * If set to `0`, the shadow's edge is sharp.
 *
 * @param spreadRadius Positive values will cause the shadow to expand and grow
 * bigger, negative values will cause the shadow to shrink.
 *
 * @param offset Offsets the shadow from the box.
 *
 * @param shape The shape of the box, which is applied to the shadow as well.
 *
 * @param clip Whether to clip the content to [shape].
 *
 * @param inset Whether the shadow should be inset to [shape]; otherwise, it is
 * a drop shadow.
 *
 * @exception IllegalArgumentException Any of the following conditions holds:
 * - [color] is [Color.Unspecified],
 * - [blurRadius] is [Dp.Unspecified] or negative,
 * - [spreadRadius] is [Dp.Unspecified],
 * - [offset] is [DpOffset.Unspecified].
 */
@Stable
fun Modifier.boxShadow(
    color: Color,
    blurRadius: Dp,
    spreadRadius: Dp = 0.dp,
    offset: DpOffset = DpOffset.Zero,
    shape: Shape = RectangleShape,
    clip: Boolean = true,
    inset: Boolean = false
): Modifier {

    require(color.isSpecified) { "color must be specified." }
    require(blurRadius.isSpecified) { "blurRadius must be specified." }
    require(spreadRadius.isSpecified) { "spreadRadius must be specified." }
    require(blurRadius.value >= 0f) { "blurRadius can't be negative." }
    require(offset.isSpecified) { "offset must be specified." }

    return drawWithCache {
        onDrawWithContent {

            if (inset)
                drawContent()

            drawIntoCanvas { canvas ->

                val colorArgb = color.toArgb()
                val hasBlurRadius = blurRadius.value.let { it.isFinite() && it != 0f }
                val paint = Paint()

                paint.asFrameworkPaint().let { frameworkPaint ->

                    if (hasBlurRadius) {
                        frameworkPaint.maskFilter = BlurMaskFilter(
                            blurRadius.toPx(),
                            BlurMaskFilter.Blur.NORMAL
                        )
                    }

                    frameworkPaint.color = colorArgb
                }

                val spreadRadiusPx = spreadRadius.toPx().let { spreadRadiusPx ->
                    when {
                        inset -> -spreadRadiusPx
                        else -> spreadRadiusPx
                    }
                }

                val hasSpreadRadius = spreadRadiusPx != 0f
                val size = size
                val layoutDirection = layoutDirection

                val density = Density(
                    density = density,
                    fontScale = fontScale
                )

                val shadowOutline = shape.createOutline(
                    size = when {
                        hasSpreadRadius -> size.let { (width, height) ->
                            (2 * spreadRadiusPx).let { outset ->
                                Size(
                                    width = width + outset,
                                    height = height + outset
                                )
                            }
                        }
                        else -> size
                    },
                    layoutDirection = layoutDirection,
                    density = density
                )

                val nativeCanvas = canvas.nativeCanvas
                val count = nativeCanvas.save()

                if (inset) {

                    val boxOutline = when {
                        hasSpreadRadius -> shape.createOutline(
                            size = size,
                            layoutDirection = layoutDirection,
                            density = density
                        )
                        else -> shadowOutline
                    }

                    canvas.clipToOutline(boxOutline)

                    val bounds = boxOutline.bounds

                    nativeCanvas.saveLayer(
                        bounds.left,
                        bounds.top,
                        bounds.right,
                        bounds.bottom,
                        NativePaint().apply {
                            colorFilter = ColorMatrixColorFilter(
                                ColorMatrix(
                                    floatArrayOf(
                                        1f, 0f, 0f, 0f, 0f,
                                        0f, 1f, 0f, 0f, 0f,
                                        0f, 0f, 1f, 0f, 0f,
                                        0f, 0f, 0f, -1f, 255f * color.alpha
                                    )
                                )
                            )
                        }
                    )
                }

                canvas.translate(
                    dx = offset.x.toPx() - spreadRadiusPx,
                    dy = offset.y.toPx() - spreadRadiusPx
                )

                canvas.drawOutline(
                    outline = shadowOutline,
                    paint = paint
                )

                nativeCanvas.restoreToCount(count)
            }

            if (!inset)
                drawContent()
        }
    }.run {
        when {
            clip -> clip(shape)
            else -> this
        }
    }
}

private fun Canvas.clipToOutline(
    outline: Outline,
    clipOp: ClipOp = ClipOp.Intersect
) {
    when (outline) {
        is Outline.Generic ->
            clipPath(path = outline.path, clipOp = clipOp)
        is Outline.Rectangle ->
            clipRect(rect = outline.rect, clipOp = clipOp)
        is Outline.Rounded ->
            clipPath(
                path = Path()
                    .apply { addRoundRect(outline.roundRect) },
                clipOp = clipOp
            )
    }
}