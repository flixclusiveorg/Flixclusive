package com.flixclusive.core.ui.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState

val LocalIsInPipMode = compositionLocalOf<Boolean> {
    error("LocalIsInPipMode is not provided")
}

@Composable
fun rememberPipMode() = rememberUpdatedState(LocalIsInPipMode.current)