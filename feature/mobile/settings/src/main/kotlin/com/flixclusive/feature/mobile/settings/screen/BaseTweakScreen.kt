package com.flixclusive.feature.mobile.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.flixclusive.feature.mobile.settings.Tweak

interface BaseTweakScreen {
    @Composable
    @ReadOnlyComposable
    fun getTitle(): String

    @Composable
    @ReadOnlyComposable
    fun getDescription(): String

    @Composable
    fun getTweaks(): List<Tweak>

    @Composable
    fun Content()
}