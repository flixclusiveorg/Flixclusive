package com.flixclusive.feature.mobile.settings.screen

import androidx.compose.runtime.Composable
import com.flixclusive.core.ui.common.navigation.navigator.SettingsScreenNavigator
import com.flixclusive.feature.mobile.settings.Tweak

internal interface BaseTweakNavigation : BaseTweakScreen {
    fun onClick(navigator: SettingsScreenNavigator)

    @Composable
    override fun getTweaks(): List<Tweak> = listOf()

    @Composable
    override fun Content() = Unit

    @Composable
    override fun getDescription() = ""
}