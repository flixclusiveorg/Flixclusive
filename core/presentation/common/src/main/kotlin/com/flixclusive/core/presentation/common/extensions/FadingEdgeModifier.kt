package com.flixclusive.core.presentation.common.extensions

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import org.intellij.lang.annotations.Language
import kotlin.math.min

/**
 * https://github.com/nikonovmi/compose-fading-edges
 */

// Vertical shaders (top/bottom) - with alpha uniform for smooth blending
@Language(value = "AGSL")
private val bottomFadingEdgeShader = """
    const half4 BLACK_COLOR = half4(0, 0, 0, 1);
    uniform float2 resolution;
    uniform float bottomFade;
    uniform float fadeAlpha;

    half4 main(float2 coord) {
        if (bottomFade < 1 || fadeAlpha <= 0.0) {
            return BLACK_COLOR;
        } else if (coord.y < resolution.y - bottomFade) {
            return BLACK_COLOR;
        } else {
            float x = ((resolution.y - coord.y) / bottomFade);
            float y = (1.0 - x) * (1.0 - x) * (1.0 - x) + 3.0 * (1.0 - x) * (1.0 - x) * x;
            float baseAlpha = 1.0 - y * y;
            // Blend between no fade (alpha=1) and full fade based on fadeAlpha
            float finalAlpha = mix(1.0, baseAlpha, fadeAlpha);
            return half4(0, 0, 0, finalAlpha);
        }
    }
"""

@Language(value = "AGSL")
private val topFadingEdgeShader = """
    const half4 BLACK_COLOR = half4(0, 0, 0, 1);
    uniform float2 resolution;
    uniform float topFade;
    uniform float fadeAlpha;

    half4 main(float2 coord) {
        if (topFade < 1 || fadeAlpha <= 0.0) {
            return BLACK_COLOR;
        } else if (coord.y > topFade) {
            return BLACK_COLOR;
        } else {
            float x = coord.y / topFade;
            float y = (1.0 - x)*(1.0 - x)*(1.0 - x) + 3.0 * (1.0 - x) * (1.0 - x) * x;
            float baseAlpha = 1.0 - y * y;
            float finalAlpha = mix(1.0, baseAlpha, fadeAlpha);
            return half4(0, 0, 0, finalAlpha);
        }
    }
"""

// Horizontal shaders (start/end)
@Language(value = "AGSL")
private val startFadingEdgeShader = """
    const half4 BLACK_COLOR = half4(0, 0, 0, 1);
    uniform float2 resolution;
    uniform float startFade;
    uniform float fadeAlpha;

    half4 main(float2 coord) {
        if (startFade < 1 || fadeAlpha <= 0.0) {
            return BLACK_COLOR;
        } else if (coord.x > startFade) {
            return BLACK_COLOR;
        } else {
            float x = coord.x / startFade;
            float y = (1.0 - x)*(1.0 - x)*(1.0 - x) + 3.0 * (1.0 - x) * (1.0 - x) * x;
            float baseAlpha = 1.0 - y * y;
            float finalAlpha = mix(1.0, baseAlpha, fadeAlpha);
            return half4(0, 0, 0, finalAlpha);
        }
    }
"""

@Language(value = "AGSL")
private val endFadingEdgeShader = """
    const half4 BLACK_COLOR = half4(0, 0, 0, 1);
    uniform float2 resolution;
    uniform float endFade;
    uniform float fadeAlpha;

    half4 main(float2 coord) {
        if (endFade < 1 || fadeAlpha <= 0.0) {
            return BLACK_COLOR;
        } else if (coord.x < resolution.x - endFade) {
            return BLACK_COLOR;
        } else {
            float x = ((resolution.x - coord.x) / endFade);
            float y = (1.0 - x) * (1.0 - x) * (1.0 - x) + 3.0 * (1.0 - x) * (1.0 - x) * x;
            float baseAlpha = 1.0 - y * y;
            float finalAlpha = mix(1.0, baseAlpha, fadeAlpha);
            return half4(0, 0, 0, finalAlpha);
        }
    }
"""

fun Modifier.fadingEdge(
    scrollableState: ScrollableState,
    orientation: Orientation,
    edgeSize: Dp = 22.dp,
) = fadingEdge(
    scrollableState = scrollableState,
    orientation = orientation,
    startEdge = edgeSize,
    endEdge = edgeSize,
)

/**
 * Adds fading edges to a scrollable container.
 *
 * @param scrollableState The scrollable state to observe for scroll position
 * @param orientation The scroll orientation (Vertical or Horizontal)
 * @param startEdge For vertical: top edge height. For horizontal: start edge width
 * @param endEdge For vertical: bottom edge height. For horizontal: end edge width
 */
fun Modifier.fadingEdge(
    scrollableState: ScrollableState,
    orientation: Orientation,
    startEdge: Dp = 22.dp,
    endEdge: Dp = 22.dp,
) = this then FadingEdgeElement(
    scrollableState = scrollableState,
    orientation = orientation,
    startEdge = startEdge,
    endEdge = endEdge,
)

private data class FadingEdgeElement(
    private val scrollableState: ScrollableState,
    private val orientation: Orientation,
    private val startEdge: Dp,
    private val endEdge: Dp,
) : ModifierNodeElement<FadingEdgeNode>() {
    override fun create() = FadingEdgeNode(
        scrollableState = scrollableState,
        orientation = orientation,
        startEdge = startEdge,
        endEdge = endEdge,
    )

    override fun update(node: FadingEdgeNode) {
        node.scrollableState = scrollableState
        node.orientation = orientation
        node.startEdge = startEdge
        node.endEdge = endEdge
        node.onUpdateParams()
    }
}

private class FadingEdgeNode(
    var scrollableState: ScrollableState,
    var orientation: Orientation,
    var startEdge: Dp,
    var endEdge: Dp,
) : Modifier.Node(), LayoutAwareModifierNode, DrawModifierNode {

    private var size = Size.Zero
    private var layoutDirection = LayoutDirection.Ltr

    // Cached shaders for API 33+
    private var topEdgeShader: RuntimeShader? = null
    private var bottomEdgeShader: RuntimeShader? = null
    private var startEdgeShader: RuntimeShader? = null
    private var endEdgeShader: RuntimeShader? = null
    private var topEdgeBrush: ShaderBrush? = null
    private var bottomEdgeBrush: ShaderBrush? = null
    private var startEdgeBrush: ShaderBrush? = null
    private var endEdgeBrush: ShaderBrush? = null

    override fun onRemeasured(size: IntSize) {
        this.size = Size(size.width.toFloat(), size.height.toFloat())
        invalidateShaders()
    }

    fun onUpdateParams() {
        invalidateShaders()
    }

    private fun invalidateShaders() {
        if (Build.VERSION.SDK_INT >= 33 && size != Size.Zero) {
            initShaders()
        }
    }

    @RequiresApi(33)
    private fun initShaders() {
        topEdgeShader = RuntimeShader(topFadingEdgeShader).also {
            it.setFloatUniform("resolution", size.width, size.height)
        }
        topEdgeBrush = ShaderBrush(topEdgeShader!!)

        bottomEdgeShader = RuntimeShader(bottomFadingEdgeShader).also {
            it.setFloatUniform("resolution", size.width, size.height)
        }
        bottomEdgeBrush = ShaderBrush(bottomEdgeShader!!)

        startEdgeShader = RuntimeShader(startFadingEdgeShader).also {
            it.setFloatUniform("resolution", size.width, size.height)
        }
        startEdgeBrush = ShaderBrush(startEdgeShader!!)

        endEdgeShader = RuntimeShader(endFadingEdgeShader).also {
            it.setFloatUniform("resolution", size.width, size.height)
        }
        endEdgeBrush = ShaderBrush(endEdgeShader!!)
    }

    /**
     * Calculates the alpha value for the fading edge based on scroll offset.
     * Uses lerp to smoothly interpolate between 0 and 1 as the user scrolls.
     *
     * @param scrollOffset The current scroll offset in pixels
     * @param edgeSizePx The size of the fading edge in pixels
     * @return Alpha value between 0f and 1f
     */
    private fun calculateAlphaFromScroll(scrollOffset: Float, edgeSizePx: Float): Float {
        if (edgeSizePx <= 0f) return 0f
        val progress = min(scrollOffset / edgeSizePx, 1f).coerceAtLeast(0f)
        return lerp(0f, 1f, progress)
    }

    /**
     * Gets the start scroll offset from the scrollable state.
     * Returns 0f if the state type doesn't support offset retrieval.
     */
    private fun getStartScrollOffset(): Float {
        return when (val state = scrollableState) {
            is ScrollState -> state.value.toFloat()
            is LazyListState -> {
                if (state.firstVisibleItemIndex == 0) {
                    state.firstVisibleItemScrollOffset.toFloat()
                } else {
                    // Already scrolled past first item, show full fade
                    Float.MAX_VALUE
                }
            }
            is LazyGridState -> {
                if (state.firstVisibleItemIndex == 0) {
                    state.firstVisibleItemScrollOffset.toFloat()
                } else {
                    Float.MAX_VALUE
                }
            }
            is LazyStaggeredGridState -> {
                if (state.firstVisibleItemIndex == 0) {
                    state.firstVisibleItemScrollOffset.toFloat()
                } else {
                    Float.MAX_VALUE
                }
            }
            else -> if (state.canScrollBackward) Float.MAX_VALUE else 0f
        }
    }

    /**
     * Gets the end scroll offset from the scrollable state.
     * Returns 0f if at the end, positive value otherwise.
     */
    private fun getEndScrollOffset(containerSize: Float): Float {
        return when (val state = scrollableState) {
            is ScrollState -> (state.maxValue - state.value).toFloat().coerceAtLeast(0f)
            is LazyListState -> {
                val layoutInfo = state.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems == 0) return 0f

                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return 0f
                val isLastItemVisible = lastVisibleItem.index == totalItems - 1

                if (isLastItemVisible) {
                    // Calculate how much of the last item extends beyond the viewport
                    val lastItemEnd = lastVisibleItem.offset + lastVisibleItem.size
                    val viewportEnd = layoutInfo.viewportEndOffset
                    (lastItemEnd - viewportEnd).toFloat().coerceAtLeast(0f)
                } else {
                    // Not at the end, show full fade
                    Float.MAX_VALUE
                }
            }
            is LazyGridState -> {
                val layoutInfo = state.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems == 0) return 0f

                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return 0f
                val isLastItemVisible = lastVisibleItem.index == totalItems - 1

                if (isLastItemVisible) {
                    val lastItemEnd = lastVisibleItem.offset.let {
                        if (orientation == Orientation.Vertical) it.y else it.x
                    } + lastVisibleItem.size.let {
                        if (orientation == Orientation.Vertical) it.height else it.width
                    }
                    val viewportEnd = layoutInfo.viewportEndOffset
                    (lastItemEnd - viewportEnd).toFloat().coerceAtLeast(0f)
                } else {
                    Float.MAX_VALUE
                }
            }
            is LazyStaggeredGridState -> {
                val layoutInfo = state.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems == 0) return 0f

                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return 0f
                val isLastItemVisible = lastVisibleItem.index == totalItems - 1

                if (isLastItemVisible) {
                    val lastItemEnd = lastVisibleItem.offset.let {
                        if (orientation == Orientation.Vertical) it.y else it.x
                    } + lastVisibleItem.size.let {
                        if (orientation == Orientation.Vertical) it.height else it.width
                    }
                    val viewportEnd = layoutInfo.viewportEndOffset
                    (lastItemEnd - viewportEnd).toFloat().coerceAtLeast(0f)
                } else {
                    Float.MAX_VALUE
                }
            }
            else -> if (state.canScrollForward) Float.MAX_VALUE else 0f
        }
    }

    override fun ContentDrawScope.draw() {
        val isVertical = orientation == Orientation.Vertical

        val startEdgePx = startEdge.toPx()
        val endEdgePx = endEdge.toPx()

        val containerSize = if (isVertical) size.height else size.width

        // Calculate alpha based on scroll offset using lerp
        val startScrollOffset = getStartScrollOffset()
        val endScrollOffset = getEndScrollOffset(containerSize)

        val startAlpha = calculateAlphaFromScroll(startScrollOffset, startEdgePx)
        val endAlpha = calculateAlphaFromScroll(endScrollOffset, endEdgePx)

        // If no fading needed, just draw content normally
        if (startAlpha <= 0f && endAlpha <= 0f) {
            drawContent()
            return
        }

        if (Build.VERSION.SDK_INT >= 33) {
            drawApi33(isVertical, startEdgePx, endEdgePx, startAlpha, endAlpha)
        } else {
            drawOldApi(isVertical, startEdgePx, endEdgePx, startAlpha, endAlpha)
        }
    }

    @RequiresApi(33)
    private fun ContentDrawScope.drawApi33(
        isVertical: Boolean,
        startEdgePx: Float,
        endEdgePx: Float,
        startAlpha: Float,
        endAlpha: Float,
    ) {
        drawContext.canvas.saveLayer(
            Rect(0f, 0f, size.width, size.height),
            Paint()
        )

        drawContent()

        if (isVertical) {
            if (startEdgePx < size.height && startAlpha > 0f) {
                topEdgeShader?.setFloatUniform("topFade", startEdgePx)
                topEdgeShader?.setFloatUniform("fadeAlpha", startAlpha)
                topEdgeBrush?.let {
                    drawRect(brush = it, blendMode = BlendMode.DstIn)
                }
            }
            if (endEdgePx < size.height && endAlpha > 0f) {
                bottomEdgeShader?.setFloatUniform("bottomFade", endEdgePx)
                bottomEdgeShader?.setFloatUniform("fadeAlpha", endAlpha)
                bottomEdgeBrush?.let {
                    drawRect(brush = it, blendMode = BlendMode.DstIn)
                }
            }
        } else {
            val isRtl = layoutDirection == LayoutDirection.Rtl
            val leftAlpha = if (isRtl) endAlpha else startAlpha
            val rightAlpha = if (isRtl) startAlpha else endAlpha

            if (startEdgePx < size.width && leftAlpha > 0f) {
                startEdgeShader?.setFloatUniform("startFade", startEdgePx)
                startEdgeShader?.setFloatUniform("fadeAlpha", leftAlpha)
                startEdgeBrush?.let {
                    drawRect(brush = it, blendMode = BlendMode.DstIn)
                }
            }
            if (endEdgePx < size.width && rightAlpha > 0f) {
                endEdgeShader?.setFloatUniform("endFade", endEdgePx)
                endEdgeShader?.setFloatUniform("fadeAlpha", rightAlpha)
                endEdgeBrush?.let {
                    drawRect(brush = it, blendMode = BlendMode.DstIn)
                }
            }
        }

        drawContext.canvas.restore()
    }

    private fun ContentDrawScope.drawOldApi(
        isVertical: Boolean,
        startEdgePx: Float,
        endEdgePx: Float,
        startAlpha: Float,
        endAlpha: Float,
    ) {
        drawContext.canvas.saveLayer(
            Rect(0f, 0f, size.width, size.height),
            Paint()
        )

        drawContent()

        if (isVertical) {
            if (startEdgePx < size.height && startAlpha > 0f) {
                // Blend between full opacity (no fade) and transparent based on startAlpha
                val fadeColor = Color.Black.copy(alpha = 1f - startAlpha)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(fadeColor, Color.Black),
                        startY = 0f,
                        endY = startEdgePx,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (endEdgePx < size.height && endAlpha > 0f) {
                val fadeColor = Color.Black.copy(alpha = 1f - endAlpha)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, fadeColor),
                        startY = size.height - endEdgePx,
                        endY = size.height,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        } else {
            val isRtl = layoutDirection == LayoutDirection.Rtl
            val leftAlpha = if (isRtl) endAlpha else startAlpha
            val rightAlpha = if (isRtl) startAlpha else endAlpha

            if (startEdgePx < size.width && leftAlpha > 0f) {
                val fadeColor = Color.Black.copy(alpha = 1f - leftAlpha)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(fadeColor, Color.Black),
                        startX = 0f,
                        endX = startEdgePx,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (endEdgePx < size.width && rightAlpha > 0f) {
                val fadeColor = Color.Black.copy(alpha = 1f - rightAlpha)
                drawRect(
                    blendMode = BlendMode.DstIn,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black, fadeColor),
                        startX = size.width - endEdgePx,
                        endX = size.width,
                    ),
                )
            }
        }

        drawContext.canvas.restore()
    }
}

@Deprecated(
    message = "Use fadingEdge with ScrollableState instead",
    replaceWith = ReplaceWith("fadingEdge(scrollableState, orientation, startEdge, endEdge)"),
)
fun Modifier.fadingEdge(brush: Brush) =
    this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            drawRect(brush = brush, blendMode = BlendMode.DstIn)
        }
