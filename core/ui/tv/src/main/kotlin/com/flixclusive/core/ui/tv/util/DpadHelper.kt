package com.flixclusive.core.ui.tv.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

fun hasPressedLeft(keyEvent: KeyEvent): Boolean {
    return keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyUp
}

fun hasPressedRight(keyEvent: KeyEvent): Boolean {
    return keyEvent.key == Key.DirectionRight && keyEvent.type == KeyEventType.KeyUp
}

data class DirectionalFocusRequester(
    val top: FocusRequester = FocusRequester(),
    val left: FocusRequester = FocusRequester(),
    val bottom: FocusRequester = FocusRequester(),
    val right: FocusRequester = FocusRequester()
)
private val LocalDirectionalFocusRequester = compositionLocalOf { DirectionalFocusRequester() }

@Composable
fun useLocalDirectionalFocusRequester() = LocalDirectionalFocusRequester.current

@Composable
fun LocalDirectionalFocusRequesterProvider(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        value = LocalDirectionalFocusRequester provides remember { DirectionalFocusRequester() },
        content = content
    )
}