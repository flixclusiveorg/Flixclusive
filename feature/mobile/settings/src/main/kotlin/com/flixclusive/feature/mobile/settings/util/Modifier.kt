package com.flixclusive.feature.mobile.settings.util

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.betterClickable(
    interactionSource: MutableInteractionSource,
    onClick: (() -> Unit)?,
    enabled: () -> Boolean,
): Modifier {
    return pointerInput(onClick, enabled) {
        detectTapGestures(
            onPress = { offset ->
                if (enabled() && onClick != null) {
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    val released = tryAwaitRelease()
                    interactionSource.emit(
                        if (released) {
                            PressInteraction.Release(press)
                        } else {
                            PressInteraction.Cancel(press)
                        },
                    )
                    if (released) {
                        onClick()
                    }
                }
            },
        )
    }
}
