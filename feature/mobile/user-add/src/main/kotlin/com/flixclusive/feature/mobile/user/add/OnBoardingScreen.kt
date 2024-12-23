package com.flixclusive.feature.mobile.user.add

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flixclusive.core.locale.UiText

internal interface OnBoardingScreen {
    val index: Int
    val title: UiText
    val description: UiText

    @Composable
    fun Content(modifier: Modifier = Modifier) {

    }

    @Composable
    fun OnBoardingIcon()
}