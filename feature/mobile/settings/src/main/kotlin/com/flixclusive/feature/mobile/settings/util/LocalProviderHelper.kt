package com.flixclusive.feature.mobile.settings.util

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberUpdatedState
import com.flixclusive.feature.mobile.settings.SettingsViewModel
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.AppSettingsProvider

internal object LocalProviderHelper {
    val LocalAppSettings = compositionLocalOf { AppSettings() }
    val LocalAppSettingsChanger = compositionLocalOf<((AppSettings) -> Unit)?> { null }

    val LocalAppSettingsProvider = compositionLocalOf { AppSettingsProvider() }
    val LocalAppSettingsProviderChanger = compositionLocalOf<((AppSettingsProvider) -> Unit)?> { null }

    val LocalSettingsViewModel = compositionLocalOf<SettingsViewModel?> { null }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    val LocalScaffoldNavigator = compositionLocalOf<ThreePaneScaffoldNavigator<BaseTweakScreen>?> { null }

    @Composable
    fun rememberAppSettingsChanger(): State<((AppSettings) -> Unit)> {
        val localSettingsChanger = LocalAppSettingsChanger.current
        check(localSettingsChanger != null) {
            "LocalAppSettingsChanger not provided"
        }

        return rememberUpdatedState(localSettingsChanger)
    }

    @Composable
    fun rememberAppSettingsProviderChanger(): State<((AppSettingsProvider) -> Unit)> {
        val localSettingsChanger = LocalAppSettingsProviderChanger.current
        check(localSettingsChanger != null) {
            "LocalSettingsChanger not provided"
        }

        return rememberUpdatedState(localSettingsChanger)
    }

    @Composable
    fun getCurrentSettingsViewModel(): SettingsViewModel {
        val localSettingsChanger = LocalSettingsViewModel.current
        check(localSettingsChanger != null) {
            "LocalSettingsViewModel not provided"
        }

        return localSettingsChanger
    }
}