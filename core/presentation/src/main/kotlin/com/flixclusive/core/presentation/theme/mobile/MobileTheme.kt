package com.flixclusive.core.presentation.theme.mobile

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.flixclusive.core.presentation.theme.md_theme_dark_error
import com.flixclusive.core.presentation.theme.md_theme_dark_errorContainer
import com.flixclusive.core.presentation.theme.md_theme_dark_inverseOnSurface
import com.flixclusive.core.presentation.theme.md_theme_dark_inversePrimary
import com.flixclusive.core.presentation.theme.md_theme_dark_inverseSurface
import com.flixclusive.core.presentation.theme.md_theme_dark_onError
import com.flixclusive.core.presentation.theme.md_theme_dark_onErrorContainer
import com.flixclusive.core.presentation.theme.md_theme_dark_onPrimary
import com.flixclusive.core.presentation.theme.md_theme_dark_onPrimaryContainer
import com.flixclusive.core.presentation.theme.md_theme_dark_onSecondary
import com.flixclusive.core.presentation.theme.md_theme_dark_onSecondaryContainer
import com.flixclusive.core.presentation.theme.md_theme_dark_onTertiary
import com.flixclusive.core.presentation.theme.md_theme_dark_onTertiaryContainer
import com.flixclusive.core.presentation.theme.md_theme_dark_outline
import com.flixclusive.core.presentation.theme.md_theme_dark_outlineVariant
import com.flixclusive.core.presentation.theme.md_theme_dark_primary
import com.flixclusive.core.presentation.theme.md_theme_dark_primaryContainer
import com.flixclusive.core.presentation.theme.md_theme_dark_scrim
import com.flixclusive.core.presentation.theme.md_theme_dark_secondary
import com.flixclusive.core.presentation.theme.md_theme_dark_secondaryContainer
import com.flixclusive.core.presentation.theme.md_theme_dark_surfaceTint
import com.flixclusive.core.presentation.theme.md_theme_dark_tertiary
import com.flixclusive.core.presentation.theme.md_theme_dark_tertiaryContainer
import com.flixclusive.core.presentation.theme.md_theme_light_background
import com.flixclusive.core.presentation.theme.md_theme_light_error
import com.flixclusive.core.presentation.theme.md_theme_light_errorContainer
import com.flixclusive.core.presentation.theme.md_theme_light_inverseOnSurface
import com.flixclusive.core.presentation.theme.md_theme_light_inversePrimary
import com.flixclusive.core.presentation.theme.md_theme_light_inverseSurface
import com.flixclusive.core.presentation.theme.md_theme_light_onBackground
import com.flixclusive.core.presentation.theme.md_theme_light_onError
import com.flixclusive.core.presentation.theme.md_theme_light_onErrorContainer
import com.flixclusive.core.presentation.theme.md_theme_light_onPrimary
import com.flixclusive.core.presentation.theme.md_theme_light_onPrimaryContainer
import com.flixclusive.core.presentation.theme.md_theme_light_onSecondary
import com.flixclusive.core.presentation.theme.md_theme_light_onSecondaryContainer
import com.flixclusive.core.presentation.theme.md_theme_light_onSurface
import com.flixclusive.core.presentation.theme.md_theme_light_onSurfaceVariant
import com.flixclusive.core.presentation.theme.md_theme_light_onTertiary
import com.flixclusive.core.presentation.theme.md_theme_light_onTertiaryContainer
import com.flixclusive.core.presentation.theme.md_theme_light_outline
import com.flixclusive.core.presentation.theme.md_theme_light_outlineVariant
import com.flixclusive.core.presentation.theme.md_theme_light_primary
import com.flixclusive.core.presentation.theme.md_theme_light_primaryContainer
import com.flixclusive.core.presentation.theme.md_theme_light_scrim
import com.flixclusive.core.presentation.theme.md_theme_light_secondary
import com.flixclusive.core.presentation.theme.md_theme_light_secondaryContainer
import com.flixclusive.core.presentation.theme.md_theme_light_surface
import com.flixclusive.core.presentation.theme.md_theme_light_surfaceTint
import com.flixclusive.core.presentation.theme.md_theme_light_surfaceVariant
import com.flixclusive.core.presentation.theme.md_theme_light_tertiary
import com.flixclusive.core.presentation.theme.md_theme_light_tertiaryContainer
import com.flixclusive.core.presentation.theme.md_theme_mobile_dark_background
import com.flixclusive.core.presentation.theme.md_theme_mobile_dark_onBackground
import com.flixclusive.core.presentation.theme.md_theme_mobile_dark_onSurface
import com.flixclusive.core.presentation.theme.md_theme_mobile_dark_onSurfaceVariant
import com.flixclusive.core.presentation.theme.md_theme_mobile_dark_surface
import com.flixclusive.core.presentation.theme.md_theme_mobile_dark_surfaceVariant


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
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
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
    background = md_theme_mobile_dark_background,
    onBackground = md_theme_mobile_dark_onBackground,
    surface = md_theme_mobile_dark_surface,
    onSurface = md_theme_mobile_dark_onSurface,
    surfaceVariant = md_theme_mobile_dark_surfaceVariant,
    onSurfaceVariant = md_theme_mobile_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

@Composable
internal fun FlixclusiveMobileTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (!useDarkTheme) {
        lightColors
    } else {
        darkColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            val windowsInsetsController = WindowCompat.getInsetsController(window, view)

            windowsInsetsController.isAppearanceLightStatusBars = !useDarkTheme
            windowsInsetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MobileTypography,
        content = content,
    )
}
