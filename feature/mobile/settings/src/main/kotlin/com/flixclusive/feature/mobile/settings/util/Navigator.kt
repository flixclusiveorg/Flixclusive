package com.flixclusive.feature.mobile.settings.util

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.compositionLocalOf
import com.flixclusive.feature.mobile.settings.screen.root.SettingsScreenNavigator

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val LocalScaffoldNavigator
    = compositionLocalOf<ThreePaneScaffoldNavigator<String>?> { null }

internal val LocalSettingsNavigator
    = compositionLocalOf<SettingsScreenNavigator?> { null }
