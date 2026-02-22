package com.flixclusive.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ramcosta.composedestinations.spec.DirectionNavGraphSpec

internal const val ROOT = "root"

@Immutable
internal data class AppNavigationItem(
    val screen: DirectionNavGraphSpec,
    @param:DrawableRes val iconSelected: Int,
    @param:DrawableRes val iconUnselected: Int,
    @param:StringRes val label: Int
)
