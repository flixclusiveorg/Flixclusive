package com.flixclusive.feature.tv.home.component.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private val LocalBackgroundImmersiveColor = compositionLocalOf<MutableState<Color?>> {
    error("LocalBackgroundColorSetter is not provided")
}

@Composable
internal fun LocalImmersiveBackgroundColorProvider(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        value = LocalBackgroundImmersiveColor provides remember { mutableStateOf(null) },
        content = content
    )
}

@Composable
internal fun useLocalImmersiveBackgroundColor() = LocalBackgroundImmersiveColor.current