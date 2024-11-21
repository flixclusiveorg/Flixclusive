package com.flixclusive.feature.mobile.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

internal data class PreferencesNavigationItem(
    @DrawableRes val iconId: Int,
    @StringRes val labelId: Int,
    val navigationAction: () -> Unit,
)