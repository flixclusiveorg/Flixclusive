package com.flixclusive

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ramcosta.composedestinations.spec.NavGraphSpec

internal const val ROOT = "root"

@Immutable
internal data class AppNavigationItem(
    val screen: NavGraphSpec,
    @DrawableRes val iconSelected: Int,
    @DrawableRes val iconUnselected: Int,
    @StringRes val label: Int
)
