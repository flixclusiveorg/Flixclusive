package com.flixclusive.feature.mobile.settings.util

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.AppSettingsProvider

internal object LocalProviderHelper {
    val LocalAppSettings = compositionLocalOf { AppSettings() }

    val LocalAppSettingsProvider = compositionLocalOf { AppSettingsProvider() }

    val LocalSettingsViewModel = compositionLocalOf<SettingsViewModel?> { null }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    val LocalScaffoldNavigator = compositionLocalOf<ThreePaneScaffoldNavigator<BaseTweakScreen>?> { null }

    @Composable
    fun getCurrentSettingsViewModel(): SettingsViewModel {
        val localSettingsChanger = LocalSettingsViewModel.current
        check(localSettingsChanger != null) {
            "LocalSettingsViewModel not provided"
        }

        return localSettingsChanger
    }
}