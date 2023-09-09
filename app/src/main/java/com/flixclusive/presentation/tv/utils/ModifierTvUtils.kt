package com.flixclusive.presentation.tv.utils

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

object ModifierTvUtils {
    data class Padding(
        val start: Dp = 0.dp,
        val top: Dp = 0.dp,
        val end: Dp = 0.dp,
        val bottom: Dp = 0.dp
    ) {
        fun getPaddingValues() = PaddingValues(start, top, end, bottom)
    }

    val LabelStartPadding = Padding(start = 16.dp)

    /**
     * Used to apply modifiers conditionally.
     */
    fun Modifier.ifElse(
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

    fun Modifier.onFilmFocusChange(
        isFocused: () -> Unit
    ) = onFocusChanged {
        if(it.isFocused) {
            isFocused()
        }
    }

    /**
     * [FocusRequesterModifiers] defines a set of modifiers which can be used for restoring focus and
     * specifying the initially focused item.
     *
     * @param [parentModifier] is added to the parent container.
     * @param [childModifier] is added to the item that needs to first gain focus.
     *
     * For example, if you want the item at index 0 to get focus for the first time,
     * you can do the following:
     *
     * LazyRow(modifier.then(modifiers.parentModifier) {
     *   item1(modifier.then(modifiers.childModifier) {...}
     *   item2 {...}
     *   item3 {...}
     *   ...
     * }
     */
    data class FocusRequesterModifiers(
        val parentModifier: Modifier,
        val childModifier: Modifier
    )

    /**
     * Returns a set of modifiers [FocusRequesterModifiers] which can be used for restoring focus and
     * specifying the initially focused item.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun createInitialFocusRestorerModifiers(): FocusRequesterModifiers {
        val focusRequester = remember { FocusRequester() }
        val childFocusRequester = remember { FocusRequester() }

        val parentModifier = Modifier
            .focusRequester(focusRequester)
            .focusProperties {
                exit = {
                    focusRequester.saveFocusedChild()
                    FocusRequester.Default
                }
                enter = {
                    if (focusRequester.restoreFocusedChild()) FocusRequester.Cancel
                    else childFocusRequester
                }
            }

        val childModifier = Modifier.focusRequester(childFocusRequester)

        return FocusRequesterModifiers(
            parentModifier = parentModifier,
            childModifier = childModifier
        )
    }

    /**
     * This modifier can be used to gain focus on a focusable component when it becomes visible
     * for the first time.
     * */
    @Composable
    fun Modifier.focusOnInitialVisibility(isVisible: MutableState<Boolean>): Modifier {
        val focusRequester = remember { FocusRequester() }

        return focusRequester(focusRequester)
            .onPlaced {
                if (!isVisible.value) {
                    focusRequester.requestFocus()
                    isVisible.value = true
                }
            }
    }

    @Composable
    fun Modifier.drawScrimOnBackground(
        gradientColor: Color = MaterialTheme.colorScheme.surface
    ) =
        drawWithCache {
            onDrawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            gradientColor,
                        ),
                        endY = size.height.times(0.9F)
                    )
                )
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            gradientColor,
                            Color.Transparent
                        ),
                        start = Offset(
                            size.width.times(0.1F),
                            size.height.times(0.3F)
                        ),
                        end = Offset(
                            size.width.times(0.75F),
                            0F
                        )
                    )
                )
            }
        }

    fun Modifier.drawAnimatedBorder(
        strokeWidth: Dp,
        shape: Shape,
        brush: Brush,
        durationMillis: Int
    ) = composed {
        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0F,
            targetValue = 360F,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "rotation"
        )

        Modifier
            .clip(shape)
            .drawWithCache {
                val strokeWidthPx = strokeWidth.toPx()

                val outline = shape.createOutline(size, layoutDirection, this)

                onDrawWithContent {
                    // This is actual content of the Composable that this modifier is assigned to
                    drawContent()

                    with(drawContext.canvas.nativeCanvas) {
                        val checkPoint = saveLayer(null, null)

                        // Destination

                        // We draw 2 times of the stroke with since we want actual size to be inside
                        // bounds while the outer stroke with is clipped with Modifier.clip

                        // Using a maskPath with op(this, outline.path, PathOperation.Difference)
                        // And GenericShape can be used as Modifier.border does instead of clip
                        drawOutline(
                            outline = outline,
                            color = Color.Gray,
                            style = Stroke(strokeWidthPx * 2)
                        )

                        // Source
                        rotate(angle) {
                            drawCircle(
                                brush = brush,
                                radius = size.width,
                                blendMode = BlendMode.SrcIn,
                            )
                        }
                        restoreToCount(checkPoint)
                    }
                }
            }
    }


    fun Modifier.glowOnFocus(
        isFocused: Boolean,
        brush: Brush
    ) = drawBehind {
        if(isFocused) {
            drawRect(brush)
        } else drawRect(Color.Transparent)
    }

    fun getGlowRadialGradient(
        color: Color
    ) = object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            val biggerDimension = maxOf(size.height, size.width)
            return RadialGradientShader(
                colors = listOf(color, Color.Transparent),
                center = size.center,
                radius = biggerDimension / 2F,
                colorStops = listOf(0F, 0.95F)
            )
        }
    }
}