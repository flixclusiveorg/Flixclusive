package com.flixclusive.core.presentation.mobile.extensions

import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

/*
* Helpers to check the current Window Size Class
*
* Based on Google's Material 3 breakpoints:
* Compact = 0 - 600dp (phones)
* Medium = 600 - 840dp (tablets, small foldables)
* Expanded = 840dp+ (larger tablets, desktops)
* */

val WindowWidthSizeClass.isCompact get() = this == WindowWidthSizeClass.COMPACT
val WindowWidthSizeClass.isMedium get() = this == WindowWidthSizeClass.MEDIUM
val WindowWidthSizeClass.isExpanded get() = this == WindowWidthSizeClass.EXPANDED

val WindowHeightSizeClass.isCompact get() = this == WindowHeightSizeClass.COMPACT
val WindowHeightSizeClass.isMedium get() = this == WindowHeightSizeClass.MEDIUM
val WindowHeightSizeClass.isExpanded get() = this == WindowHeightSizeClass.EXPANDED
