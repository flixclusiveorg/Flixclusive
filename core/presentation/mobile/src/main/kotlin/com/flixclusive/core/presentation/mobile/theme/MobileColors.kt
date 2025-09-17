package com.flixclusive.core.presentation.mobile.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.flixclusive.core.presentation.common.theme.Elevations.getElevationForLevel

/**
 * Object to hold mobile-specific color definitions if needed in the future.
 * */
object MobileColors {
    val darkBackground = Color(0xFF121212)
    val darkOnBackground = Color(0xFFEAE0E4)
    val darkSurface = Color(0xFF121212)
    val darkOnSurface = Color(0xFFEAE0E4)
    val darkSurfaceVariant = Color(0xFF282828)
    val darkOnSurfaceVariant = Color(0xFFE7E4EE)

    /**
     * Extension function to get surface color at a specific elevation.
     *
     * This is useful for creating a sense of depth in the UI.
     *
     * @param elevation The elevation level (in dp) to calculate the surface color for.
     * @param color The base color to use for the elevation effect. Defaults to onSurface color.
     * @return The color of the surface at the specified elevation.
     * */
    @Stable
    fun ColorScheme.surfaceColorAtElevation(
        elevation: Float,
        color: Color = onSurface,
    ): Color {
        if (elevation == 0f) return surface
        return color.copy(alpha = elevation.coerceIn(0f, 1f))
            .compositeOver(surface)
    }

    /**
     * Extension function to get surface color at a specific elevation.
     *
     * This is useful for creating a sense of depth in the UI.
     *
     * @param level The elevation level to calculate the surface color for. This uses [getElevationForLevel].
     * @param color The base color to use for the elevation effect. Defaults to onSurface color.
     * @return The color of the surface at the specified elevation.
     *
     * @see getElevationForLevel
     * */
    @Stable
    fun ColorScheme.surfaceColorAtElevation(
        level: Int,
        color: Color = onSurface,
    ): Color {
        val elevation = getElevationForLevel(level)

        return surfaceColorAtElevation(elevation, color)
    }

}
