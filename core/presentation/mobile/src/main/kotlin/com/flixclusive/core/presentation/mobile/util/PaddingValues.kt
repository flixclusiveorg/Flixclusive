package com.flixclusive.core.presentation.mobile.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * In order to get better UX scaffolding's custom padding must not be
 * implemented on the top-root most Box composable in the MainApp.
 *
 * This composition local should help for implementing that usecase.
 * Though, this just makes every screen composables hard to maintain.
 * */
val LocalGlobalScaffoldPadding = compositionLocalOf { PaddingValues() }

@Stable
fun PaddingValues.copy(
    start: Dp = calculateLeftPadding(LayoutDirection.Ltr),
    top: Dp = calculateTopPadding(),
    end: Dp = calculateRightPadding(LayoutDirection.Ltr),
    bottom: Dp = calculateBottomPadding(),
) = PaddingValues(
    start = start,
    top = top,
    end = end,
    bottom = bottom,
)
