package com.flixclusive.core.ui.mobile.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.compositionLocalOf

/**
 * In order to get better UX scaffolding's custom padding must not be
 * implemented on the top-root most Box composable in the MainApp.
 *
 * This composition local should help for implementing that usecase.
 * Though, this just makes every screen composables hard to maintain.
 * */
val LocalGlobalScaffoldPadding = compositionLocalOf { PaddingValues() }
