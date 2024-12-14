package com.flixclusive.feature.mobile.user.util

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

internal object ModifierUtil {
    fun Modifier.scaleDownOnPress(
        index: Int,
        pressState: MutableState<Int?>
    ) = composed {
        this
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // Scale down when pressed
                        pressState.value = index
                        tryAwaitRelease()
                        // Scale back up when released
                        pressState.value = null
                    }
                )
            }
    }

    fun GraphicsLayerScope.getPagerBlur(pageOffset: Float, ): RenderEffect? {
        val blurRadius = lerp(
            start = 10F,
            stop = 0F,
            fraction = 1F - pageOffset
        ).dp.toPx()
        val tileMode = TileMode.Clamp
        return if (blurRadius > 0f) {
            BlurEffect(blurRadius, blurRadius, tileMode)
        } else null
    }

    fun GraphicsLayerScope.getPagerScale(pageOffset: Float): Float {
        return lerp(
            start = 0.7f,
            stop = 1f,
            fraction = 1f - pageOffset
        )
    }
}