package com.flixclusive.core.presentation.common.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.flixclusive.core.strings.R as LocaleR

/**
 * A centralized object to manage and access the color palette used throughout the application.
 * This includes predefined colors for various UI elements, as well as theme-specific colors
 * for light and dark modes.
 * */
object Colors {
    val lightGray = Color.LightGray.copy(0.2F)
    val lightGrayElevated = Color.LightGray.copy(0.4F)
    val starColor = Color(0xFFFFD65B)
    val warningColor = Color(0xFFFFD54F)

    val lightPrimary = Color(0xFF006689)
    val lightOnPrimary = Color(0xFFFFFFFF)
    val lightPrimaryContainer = Color(0xFFC3E8FF)
    val lightOnPrimaryContainer = Color(0xFF001E2C)
    val lightSecondary = Color(0xFF4E616D)
    val lightOnSecondary = Color(0xFFFFFFFF)
    val lightSecondaryContainer = Color(0xFFD1E5F3)
    val lightOnSecondaryContainer = Color(0xFF091E28)
    val lightTertiary = Color(0xFF605A7D)
    val lightOnTertiary = Color(0xFFFFFFFF)
    val lightTertiaryContainer = Color(0xFFE6DEFF)
    val lightOnTertiaryContainer = Color(0xFF1C1736)
    val lightError = Color(0xFFBA1A1A)
    val lightErrorContainer = Color(0xFFFFDAD6)
    val lightOnError = Color(0xFFFFFFFF)
    val lightOnErrorContainer = Color(0xFF410002)
    val lightBackground = Color(0xFFFBFCFE)
    val lightOnBackground = Color(0xFF191C1E)
    val lightSurface = Color(0xFFFBFCFE)
    val lightOnSurface = Color(0xFF191C1E)
    val lightSurfaceVariant = Color(0xFFDCE3E9)
    val lightOnSurfaceVariant = Color(0xFF41484D)
    val lightOutline = Color(0xFF71787D)
    val lightInverseOnSurface = Color(0xFFF0F1F3)
    val lightInverseSurface = Color(0xFF2E3133)
    val lightInversePrimary = Color(0xFF78D1FF)
    //val lightShadow = Color(0xFF000000)
    val lightSurfaceTint = Color(0xFF006689)
    val lightOutlineVariant = Color(0xFFC0C7CD)
    val lightScrim = Color(0xFF000000)

    val darkPrimary = Color(0xFFB39DDB)
    val darkOnPrimary = Color(0xFF000000)
    val darkPrimaryContainer = Color(0xFF82007C)
    val darkOnPrimaryContainer = Color(0xFFFFD7F3)
    val darkSecondary = Color(0xFF7CD0FF)
    val darkOnSecondary = Color(0xFF00344A)
    val darkSecondaryContainer = Color(0xFF004C69)
    val darkOnSecondaryContainer = Color(0xFFC4E7FF)
    val darkTertiary = Color(0xFF84FFFF)
    val darkOnTertiary = Color(0xFF000000)
    val darkTertiaryContainer = Color(0xFF4527A0)
    val darkOnTertiaryContainer = Color(0xFFFFDADB)
    val darkError = Color(0xFFFFB4AB)
    val darkErrorContainer = Color(0xFF93000A)
    val darkOnError = Color(0xFF690005)
    val darkOnErrorContainer = Color(0xFFFFDAD6)
    val darkOutline = Color(0xFF9A8D95)
    val darkInverseOnSurface = Color(0xFF000000)
    val darkInverseSurface = Color(0xFFFFFFFF)
    val darkInversePrimary = Color(0xFFAB00A2)
    val darkSurfaceTint = Color(0xFFE0DEE4)
    val darkOutlineVariant = Color(0xFF4E444B)
    val darkScrim = Color(0xFF000000)

    // TODO: Move to `core-presentation-tv` module
    internal val tv_darkBackground = Color(0xFF000000)
    internal val tv_darkOnBackground = Color(0xFFFFFFFF)
    internal val tv_darkSurface = Color(0xFF000000)
    internal val tv_darkOnSurface = Color(0xFFFFFFFF)
    internal val tv_darkSurfaceVariant = Color(0xFF121212)
    internal val tv_darkOnSurfaceVariant = Color(0xFFFFFFFF)

    fun getAvailableSubtitleColors(context: Context)
        = mapOf(
        Color(0xFFFFFFFF) to context.getString(LocaleR.string.white),
        Color(0xFFFFFF00) to context.getString(LocaleR.string.yellow),
        Color(0xFF00FFFF) to context.getString(LocaleR.string.cyan),
    )

    val subtitleBackgroundColors = mapOf(
        "Transparent" to Color.Transparent,
        "White" to Color(-1),
        "Black" to Color(0xFF000000),
        "Yellow" to Color(0xFFFFFF00),
        "Cyan" to Color(0xFF00FFFF),
    )
}

