package com.flixclusive.core.presentation.mobile.theme

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
import com.flixclusive.core.presentation.common.theme.Colors

private val lightColors = lightColorScheme(
    primary = Colors.lightPrimary,
    onPrimary = Colors.lightOnPrimary,
    primaryContainer = Colors.lightPrimaryContainer,
    onPrimaryContainer = Colors.lightOnPrimaryContainer,
    secondary = Colors.lightSecondary,
    onSecondary = Colors.lightOnSecondary,
    secondaryContainer = Colors.lightSecondaryContainer,
    onSecondaryContainer = Colors.lightOnSecondaryContainer,
    tertiary = Colors.lightTertiary,
    onTertiary = Colors.lightOnTertiary,
    tertiaryContainer = Colors.lightTertiaryContainer,
    onTertiaryContainer = Colors.lightOnTertiaryContainer,
    error = Colors.lightError,
    errorContainer = Colors.lightErrorContainer,
    onError = Colors.lightOnError,
    onErrorContainer = Colors.lightOnErrorContainer,
    background = Colors.lightBackground,
    onBackground = Colors.lightOnBackground,
    surface = Colors.lightSurface,
    onSurface = Colors.lightOnSurface,
    surfaceVariant = Colors.lightSurfaceVariant,
    onSurfaceVariant = Colors.lightOnSurfaceVariant,
    outline = Colors.lightOutline,
    inverseOnSurface = Colors.lightInverseOnSurface,
    inverseSurface = Colors.lightInverseSurface,
    inversePrimary = Colors.lightInversePrimary,
    surfaceTint = Colors.lightSurfaceTint,
    outlineVariant = Colors.lightOutlineVariant,
    scrim = Colors.lightScrim,
)

private val darkColors = darkColorScheme(
    primary = Colors.darkPrimary,
    onPrimary = Colors.darkOnPrimary,
    primaryContainer = Colors.darkPrimaryContainer,
    onPrimaryContainer = Colors.darkOnPrimaryContainer,
    secondary = Colors.darkSecondary,
    onSecondary = Colors.darkOnSecondary,
    secondaryContainer = Colors.darkSecondaryContainer,
    onSecondaryContainer = Colors.darkOnSecondaryContainer,
    tertiary = Colors.darkTertiary,
    onTertiary = Colors.darkOnTertiary,
    tertiaryContainer = Colors.darkTertiaryContainer,
    onTertiaryContainer = Colors.darkOnTertiaryContainer,
    error = Colors.darkError,
    errorContainer = Colors.darkErrorContainer,
    onError = Colors.darkOnError,
    onErrorContainer = Colors.darkOnErrorContainer,
    background = MobileColors.darkBackground,
    onBackground = MobileColors.darkOnBackground,
    surface = MobileColors.darkSurface,
    onSurface = MobileColors.darkOnSurface,
    surfaceVariant = MobileColors.darkSurfaceVariant,
    onSurfaceVariant = MobileColors.darkOnSurfaceVariant,
    outline = Colors.darkOutline,
    inverseOnSurface = Colors.darkInverseOnSurface,
    inverseSurface = Colors.darkInverseSurface,
    inversePrimary = Colors.darkInversePrimary,
    surfaceTint = Colors.darkSurfaceTint,
    outlineVariant = Colors.darkOutlineVariant,
    scrim = Colors.darkScrim,
)

/**
 * Material 3 Theme for Mobile platform
 *
 * @param useDarkTheme Whether to use the dark theme. Default is true.
 * @param content The composable content to be styled with this theme.
 * */
@Composable
fun FlixclusiveTheme(
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
