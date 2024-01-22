package com.flixclusive.feature.mobile.player.controls.common.slider.util

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp


internal object SliderTokens {
    val ActiveTrackColor = ColorSchemeKeyTokens.Primary
    val DisabledActiveTrackColor = ColorSchemeKeyTokens.OnSurface
    const val DisabledActiveTrackOpacity = 0.38f
    val DisabledHandleColor = ColorSchemeKeyTokens.OnSurface
    const val DisabledHandleOpacity = 0.38f
    val DisabledInactiveTrackColor = ColorSchemeKeyTokens.OnSurface
    const val DisabledInactiveTrackOpacity = 0.12f
    val HandleColor = ColorSchemeKeyTokens.Primary
    val HandleShape = CircleShape
    val InactiveTrackColor = ColorSchemeKeyTokens.SurfaceVariant
    val InactiveTrackHeight = 4.0.dp
    val StateLayerSize = 40.0.dp
    val TickMarksActiveContainerColor = ColorSchemeKeyTokens.OnPrimary
    const val TickMarksActiveContainerOpacity = 0.38f
    val TickMarksContainerSize = 2.0.dp
    val TickMarksDisabledContainerColor = ColorSchemeKeyTokens.OnSurface
    const val TickMarksDisabledContainerOpacity = 0.38f
    val TickMarksInactiveContainerColor = ColorSchemeKeyTokens.OnSurfaceVariant
    const val TickMarksInactiveContainerOpacity = 0.38f
}