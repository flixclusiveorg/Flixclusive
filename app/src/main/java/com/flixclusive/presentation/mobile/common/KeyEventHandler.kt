package com.flixclusive.presentation.mobile.common

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState

/**
 *
 * https://stackoverflow.com/a/70843093/19371763
 *
 * */

typealias KeyEventHandler = (Int, KeyEvent) -> Boolean
val LocalKeyEventHandlers = compositionLocalOf<MutableList<KeyEventHandler>> {
    error("LocalKeyEventHandlers is not provided")
}

@Composable
fun ListenKeyEvents(handler: KeyEventHandler) {
    val handlerState = rememberUpdatedState(handler)
    val eventHandlers = LocalKeyEventHandlers.current

    DisposableEffect(handlerState) {
        val localHandler: KeyEventHandler = { code, event ->
            handlerState.value(code, event)
        }
        eventHandlers.add(localHandler)

        onDispose {
            eventHandlers.remove(localHandler)
        }
    }
}