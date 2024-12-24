package com.flixclusive.feature.mobile.user.add

import androidx.compose.runtime.Composable
import com.flixclusive.core.locale.UiText

internal interface OnBoardingScreen {
    val index: Int
    val title: UiText
    val description: UiText
    val canSkip: Boolean
        get() = false

    @Composable
    fun Content()
}