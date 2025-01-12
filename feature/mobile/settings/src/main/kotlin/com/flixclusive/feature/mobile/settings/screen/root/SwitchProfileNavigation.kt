package com.flixclusive.feature.mobile.settings.screen.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.screen.BaseTweakNavigation
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal object SwitchProfileNavigation : BaseTweakNavigation {
    @Composable
    override fun getTitle(): String
        = stringResource(LocaleR.string.switch_profile)

    @Composable
    override fun getIconPainter(): Painter
        = painterResource(UiCommonR.drawable.profile_placeholder)

    override fun onClick(navigator: SettingsScreenNavigator) {
        navigator.openProfilesScreen()
    }
}
