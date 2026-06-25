package com.flixclusive.core.presentation.mobile.extensions

import androidx.window.core.layout.WindowSizeClass

/*
* Helpers to check the current Window Size Class
*
* Based on Google's Material 3 breakpoints:
* Compact = 0 - 600dp (phones)
* Medium = 600 - 840dp (tablets, small foldables)
* Expanded = 840dp+ (larger tablets, desktops)
* */

private const val WIDTH_COMPACT_BREAKPOINT = 600
private const val WIDTH_MEDIUM_BREAKPOINT = 840

private const val HEIGHT_COMPACT_BREAKPOINT = 480
private const val HEIGHT_MEDIUM_BREAKPOINT = 900

val WindowSizeClass.isWidthCompact get() = minWidthDp < WIDTH_COMPACT_BREAKPOINT
val WindowSizeClass.isWidthMedium get() = minWidthDp < WIDTH_MEDIUM_BREAKPOINT
val WindowSizeClass.isHeightCompact get() = minHeightDp < HEIGHT_COMPACT_BREAKPOINT
val WindowSizeClass.isHeightMedium get() = minHeightDp < HEIGHT_MEDIUM_BREAKPOINT
