package com.flixclusive.core.ui.tv.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalDrawerWidth = compositionLocalOf { 50.dp }
@Composable
fun getLocalDrawerWidth() = LocalDrawerWidth.current

data class Padding(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp
) {
    constructor(
        horizontal: Dp,
        vertical: Dp,
    ) : this(
        start = horizontal, top = vertical, end = horizontal, bottom = vertical
    )

    constructor(all: Dp) : this(
        start = all, top = all, end = all, bottom = all
    )

    fun getPaddingValues() = PaddingValues(start, top, end, bottom)
}

val LabelStartPadding = Padding(start = 16.dp)