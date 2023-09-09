@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.flixclusive.presentation.tv.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.flixclusive.presentation.theme.md_theme_tv_dark_background
import com.flixclusive.presentation.theme.md_theme_dark_error
import com.flixclusive.presentation.theme.md_theme_dark_errorContainer
import com.flixclusive.presentation.theme.md_theme_dark_inverseOnSurface
import com.flixclusive.presentation.theme.md_theme_dark_inversePrimary
import com.flixclusive.presentation.theme.md_theme_dark_inverseSurface
import com.flixclusive.presentation.theme.md_theme_tv_dark_onBackground
import com.flixclusive.presentation.theme.md_theme_dark_onError
import com.flixclusive.presentation.theme.md_theme_dark_onErrorContainer
import com.flixclusive.presentation.theme.md_theme_dark_onPrimary
import com.flixclusive.presentation.theme.md_theme_dark_onPrimaryContainer
import com.flixclusive.presentation.theme.md_theme_dark_onSecondary
import com.flixclusive.presentation.theme.md_theme_dark_onSecondaryContainer
import com.flixclusive.presentation.theme.md_theme_tv_dark_onSurface
import com.flixclusive.presentation.theme.md_theme_tv_dark_onSurfaceVariant
import com.flixclusive.presentation.theme.md_theme_dark_onTertiary
import com.flixclusive.presentation.theme.md_theme_dark_onTertiaryContainer
import com.flixclusive.presentation.theme.md_theme_dark_primary
import com.flixclusive.presentation.theme.md_theme_dark_primaryContainer
import com.flixclusive.presentation.theme.md_theme_dark_scrim
import com.flixclusive.presentation.theme.md_theme_dark_secondary
import com.flixclusive.presentation.theme.md_theme_dark_secondaryContainer
import com.flixclusive.presentation.theme.md_theme_tv_dark_surface
import com.flixclusive.presentation.theme.md_theme_dark_surfaceTint
import com.flixclusive.presentation.theme.md_theme_tv_dark_surfaceVariant
import com.flixclusive.presentation.theme.md_theme_dark_tertiary
import com.flixclusive.presentation.theme.md_theme_dark_tertiaryContainer
import com.flixclusive.presentation.theme.md_theme_light_background
import com.flixclusive.presentation.theme.md_theme_light_error
import com.flixclusive.presentation.theme.md_theme_light_errorContainer
import com.flixclusive.presentation.theme.md_theme_light_inverseOnSurface
import com.flixclusive.presentation.theme.md_theme_light_inversePrimary
import com.flixclusive.presentation.theme.md_theme_light_inverseSurface
import com.flixclusive.presentation.theme.md_theme_light_onBackground
import com.flixclusive.presentation.theme.md_theme_light_onError
import com.flixclusive.presentation.theme.md_theme_light_onErrorContainer
import com.flixclusive.presentation.theme.md_theme_light_onPrimary
import com.flixclusive.presentation.theme.md_theme_light_onPrimaryContainer
import com.flixclusive.presentation.theme.md_theme_light_onSecondary
import com.flixclusive.presentation.theme.md_theme_light_onSecondaryContainer
import com.flixclusive.presentation.theme.md_theme_light_onSurface
import com.flixclusive.presentation.theme.md_theme_light_onSurfaceVariant
import com.flixclusive.presentation.theme.md_theme_light_onTertiary
import com.flixclusive.presentation.theme.md_theme_light_onTertiaryContainer
import com.flixclusive.presentation.theme.md_theme_light_primary
import com.flixclusive.presentation.theme.md_theme_light_primaryContainer
import com.flixclusive.presentation.theme.md_theme_light_scrim
import com.flixclusive.presentation.theme.md_theme_light_secondary
import com.flixclusive.presentation.theme.md_theme_light_secondaryContainer
import com.flixclusive.presentation.theme.md_theme_light_surface
import com.flixclusive.presentation.theme.md_theme_light_surfaceTint
import com.flixclusive.presentation.theme.md_theme_light_surfaceVariant
import com.flixclusive.presentation.theme.md_theme_light_tertiary
import com.flixclusive.presentation.theme.md_theme_light_tertiaryContainer

private val lightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    scrim = md_theme_light_scrim,
)


private val darkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_tv_dark_background,
    onBackground = md_theme_tv_dark_onBackground,
    surface = md_theme_tv_dark_surface,
    onSurface = md_theme_tv_dark_onSurface,
    surfaceVariant = md_theme_tv_dark_surfaceVariant,
    onSurfaceVariant = md_theme_tv_dark_onSurfaceVariant,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    scrim = md_theme_dark_scrim,
)

@Composable
fun FlixclusiveTvTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (!useDarkTheme) {
        lightColors
    } else {
        darkColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = TvTypography,
        content = content,
    )
}