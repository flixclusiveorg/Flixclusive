package com.flixclusive.feature.mobile.settings.screen.providers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.ui.common.navigation.navigator.SettingsScreenNavigator
import com.flixclusive.feature.mobile.settings.screen.BaseTweakNavigation
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal object ProvidersTweakNavigation : BaseTweakNavigation {
    @Composable
    override fun getTitle(): String
        = stringResource(LocaleR.string.providers)

    @Composable
    override fun getIconPainter(): Painter
        = painterResource(UiCommonR.drawable.provider_logo)

    override fun onClick(navigator: SettingsScreenNavigator) {
        navigator.openProvidersScreen()
    }
}