package com.flixclusive.feature.mobile.settings.screen.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.rememberAppSettingsChanger
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal class DataTweakScreen(
    private val searchHistoryCount: Int,
    private val onClearSearchHistory: () -> Unit
) : BaseTweakScreen {
    @Composable
    override fun getTitle(): String
        = stringResource(LocaleR.string.data_and_backup)

    @Composable
    override fun getIconPainter(): Painter
        = painterResource(UiCommonR.drawable.database_icon_thin)

    @Composable
    override fun getDescription(): String
        = stringResource(LocaleR.string.appearance_settings_content_desc)

    @Composable
    override fun getTweaks(): List<Tweak>
        = listOf(getSearchTweaks())

    @Composable
    override fun Content() {
        TODO("Not yet implemented")
    }

    @Composable
    private fun getSearchTweaks(): TweakGroup {
        val appSettings = LocalAppSettings.current
        val onTweaked by rememberAppSettingsChanger()

        val isIncognito = remember { mutableStateOf(appSettings.isIncognito) }

        return TweakGroup(
            title = stringResource(LocaleR.string.search),
            tweaks = persistentListOf(
                TweakUI.SwitchTweak(
                    value = isIncognito,
                    title = stringResource(LocaleR.string.pause_search_history),
                    description = stringResource(LocaleR.string.paused_search_history_content_desc),
                    onTweaked = {
                        onTweaked(appSettings.copy(isIncognito = it))
                        true
                    }
                ),
                TweakUI.ClickableTweak(
                    title = stringResource(LocaleR.string.clear_search_history),
                    description = stringResource(LocaleR.string.search_history_item_count_format, searchHistoryCount),
                    onClick = onClearSearchHistory,
                ),
            )
        )
    }
}