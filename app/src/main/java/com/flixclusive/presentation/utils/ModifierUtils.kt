package com.flixclusive.presentation.utils

import android.view.KeyEvent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.flixclusive.presentation.theme.lightGray

object ModifierUtils {
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
}