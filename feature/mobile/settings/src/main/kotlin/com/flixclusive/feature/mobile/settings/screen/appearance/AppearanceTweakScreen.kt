package com.flixclusive.feature.mobile.settings.screen.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalAppSettings
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.getCurrentSettingsViewModel
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal object AppearanceTweakScreen : BaseTweakScreen {
    @Composable
    override fun getTitle(): String
        = stringResource(LocaleR.string.appearance)

    @Composable
    override fun getIconPainter(): Painter
        = painterResource(UiCommonR.drawable.appearance_settings)

    @Composable
    override fun getDescription(): String
        = stringResource(LocaleR.string.appearance_settings_content_desc)

    @Composable
    override fun getTweaks(): List<Tweak>
        = listOf(getGeneralTweaks())

    @Composable
    private fun getGeneralTweaks(): TweakGroup {
        val appSettings = LocalAppSettings.current
        val viewModel = getCurrentSettingsViewModel()
        val onTweaked = viewModel::onChangeAppSettings

        return TweakGroup(
            title = stringResource(LocaleR.string.general),
            tweaks = persistentListOf(
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.film_card_titles),
                    description = stringResource(LocaleR.string.film_card_titles_settings_content_desc),
                    value = remember { mutableStateOf(appSettings.isShowingFilmCardTitle) },
                    onTweaked = {
                        onTweaked(appSettings.copy(isShowingFilmCardTitle = it))
                        true
                    }
                )
            )
        )
    }
}