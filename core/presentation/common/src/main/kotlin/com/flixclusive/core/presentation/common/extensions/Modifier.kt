package com.flixclusive.core.presentation.common.extensions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

/**
 * For TextField on AppBar, this modifier will request focus
 * to the element the first time it's composed.
 */
fun Modifier.showSoftKeyboard(show: Boolean): Modifier =
    if (show) {
        composed {
            val focusRequester = remember { FocusRequester() }
            var openKeyboard by rememberSaveable { mutableStateOf(show) }
            LaunchedEffect(focusRequester) {
                if (openKeyboard) {
                    focusRequester.requestFocus()
                    openKeyboard = false
                }
            }

            Modifier.focusRequester(focusRequester)
        }
    } else {
        this
    }

/**
 * For TextField, this modifier will clear focus when soft
 * keyboard is hidden.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.clearFocusOnSoftKeyboardHide(onFocusClear: (() -> Unit)? = null): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    var keyboardShowedSinceFocused by remember { mutableStateOf(false) }
    if (isFocused) {
        val imeVisible = WindowInsets.isImeVisible
        val focusManager = LocalFocusManager.current
        LaunchedEffect(imeVisible) {
            if (imeVisible) {
                keyboardShowedSinceFocused = true
            } else if (keyboardShowedSinceFocused) {
                focusManager.clearFocus()
                onFocusClear?.invoke()
            }
        }
    }

    return onFocusChanged {
        if (isFocused != it.isFocused) {
            if (isFocused) {
                keyboardShowedSinceFocused = false
            }
            isFocused = it.isFocused
        }
    }
}

fun Modifier.fadingEdge(brush: Brush) =
    this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            drawRect(brush = brush, blendMode = BlendMode.DstIn)
        }

@Composable
fun Modifier.placeholderEffect(
    shape: Shape = RoundedCornerShape(5.dp),
    color: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
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
    ifFalseModifier: Modifier = Modifier,
): Modifier = this.then(if (condition()) ifTrueModifier else ifFalseModifier)

/**
 * Used to apply modifiers conditionally.
 */
fun Modifier.ifElse(
    condition: Boolean,
    ifTrueModifier: Modifier,
    ifFalseModifier: Modifier = Modifier,
): Modifier = ifElse({ condition }, ifTrueModifier, ifFalseModifier)

/**
 * A [Modifier.clickable] without any indication (ripple, etc).
 */
@Composable
fun Modifier.noIndicationClickable(onClick: () -> Unit): Modifier {
    return clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick,
    )
}
