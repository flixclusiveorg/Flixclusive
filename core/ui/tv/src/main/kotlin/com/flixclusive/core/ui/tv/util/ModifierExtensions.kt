@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.flixclusive.core.ui.tv.util

import android.view.KeyEvent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.flixclusive.core.util.exception.safeCall

private val DPadEventsKeyCodes = listOf(
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT,
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN,
)

/**
 * Handles horizontal (Left & Right) D-Pad Keys and consumes the event(s) so that the focus doesn't
 * accidentally move to another element.
 * */
fun Modifier.handleDPadKeyEvents(
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
) = onPreviewKeyEvent {
    fun onActionUp(block: () -> Unit) {
        if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) block()
    }

    if (DPadEventsKeyCodes.contains(it.nativeKeyEvent.keyCode)) {
        when (it.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                onLeft?.apply {
                    onActionUp(::invoke)
                    return@onPreviewKeyEvent true
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                onRight?.apply {
                    onActionUp(::invoke)
                    return@onPreviewKeyEvent true
                }
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP -> {
                onUp?.apply {
                    onActionUp(::invoke)
                    return@onPreviewKeyEvent true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN -> {
                onDown?.apply {
                    onActionUp(::invoke)
                    return@onPreviewKeyEvent true
                }
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                onEnter?.apply {
                    onActionUp(::invoke)
                    return@onPreviewKeyEvent true
                }
            }
        }
    }
    false
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
                // Safe call because this one's still bugged.
                val isRestored = safeCall {
                    focusRequester.restoreFocusedChild()
                }

                when (isRestored) {
                    true -> FocusRequester.Cancel
                    null -> FocusRequester.Default // Fail-safe if compose tv acts up
                    else -> childFocusRequester
                }
            }
        }

    val childModifier = Modifier.focusRequester(childFocusRequester)

    return FocusRequesterModifiers(
        parentModifier = parentModifier,
        childModifier = childModifier
    )
}


/**
 * A workaround for compose tv 1.0.0-alpha09/10 weird bug.
 * Use this until the bug is fixed lol.
 *
 * @see createInitialFocusRestorerModifiers
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun createDefaultFocusRestorerModifier(): Modifier {
    val focusRequester = remember { FocusRequester() }

    return Modifier
        .focusRequester(focusRequester)
        .focusProperties {
            exit = {
                focusRequester.saveFocusedChild()
                FocusRequester.Default
            }
            enter = {
                // Safe call because this one's still bugged.
                val isRestored = safeCall {
                    focusRequester.restoreFocusedChild()
                }

                if (isRestored == true) FocusRequester.Cancel
                else FocusRequester.Default
            }
        }
}



/**
 * This modifier can be used to gain focus on a focusable component when it becomes visible
 * for the first time.
 * */
@Composable
fun Modifier.focusOnInitialVisibility(
    isVisible: MutableState<Boolean> = remember { mutableStateOf(false) }
): Modifier {
    val focusRequester = remember { FocusRequester() }

    return focusRequester(focusRequester)
        .onPlaced {
            if (!isVisible.value) {
                focusRequester.requestFocus()
                isVisible.value = true
            }
        }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Modifier.drawScrimOnForeground(
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
                        size.width.times(0.3F),
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Modifier.drawScrimOnBackground(
    gradientColor: Color = MaterialTheme.colorScheme.surface
) = drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                0F to Color.Transparent,
                0.8F to gradientColor,
            )
        )
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    gradientColor,
                    Color.Transparent
                ),
                start = Offset(
                    size.width.times(0.3F),
                    size.height.times(0.3F)
                ),
                end = Offset(
                    size.width.times(0.75F),
                    0F
                )
            )
        )
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

@Composable
fun Modifier.focusOnMount(
    itemKey: String,
    onFocus: (() -> Unit)? = null
): Modifier {
    val isInitialFocusTransferred = useLocalFocusTransferredOnLaunch()
    val lastFocusedItemPerDestination = useLocalLastFocusedItemPerDestination()
    val lastItemFocusedFocusRequester = useLocalLastFocusedItemFocusedRequester()
    val currentRoute = useLocalCurrentRoute()

    val focusRequester = remember { FocusRequester() }

    return this
        .focusRequester(focusRequester)
        .onGloballyPositioned {
            val lastFocusedKey = lastFocusedItemPerDestination[currentRoute]

            if (!isInitialFocusTransferred.value && lastFocusedKey == itemKey) {
                focusRequester.requestFocus()
                isInitialFocusTransferred.value = true
            }
        }
        .onFocusChanged {
            if (it.isFocused) {
                onFocus?.invoke()
                lastFocusedItemPerDestination[currentRoute] = itemKey
                isInitialFocusTransferred.value = true
                lastItemFocusedFocusRequester.value = focusRequester
            }
        }
}