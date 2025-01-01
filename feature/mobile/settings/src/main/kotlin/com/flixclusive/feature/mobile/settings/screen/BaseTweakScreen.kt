package com.flixclusive.feature.mobile.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.painter.Painter
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakScaffold

internal interface BaseTweakScreen {
    @Composable
    @ReadOnlyComposable
    fun getTitle(): String

    @Composable
    @ReadOnlyComposable
    fun getDescription(): String

    @Composable
    @ReadOnlyComposable
    fun getIconPainter(): Painter? = null

    @Composable
    fun getTweaks(): List<Tweak>

    @Composable
    fun Content() {
        TweakScaffold(
            title = getTitle(),
            description = getDescription(),
            tweaksProvider = { getTweaks() }
        )
    }
}