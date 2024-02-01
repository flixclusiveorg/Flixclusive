package com.flixclusive.feature.tv.home.component.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color

typealias ChangeImmersiveColorCallback = (Color) -> Unit
internal val LocalImmersiveColorHandler = compositionLocalOf<ChangeImmersiveColorCallback> {
    error("LocalBackgroundColorSetter is not provided")
}

@Composable
fun LocalImmersiveColorHandlerProvider(
    onColorChange: ChangeImmersiveColorCallback,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        value = LocalImmersiveColorHandler provides rememberUpdatedState(onColorChange).value,
        content = content
    )
}