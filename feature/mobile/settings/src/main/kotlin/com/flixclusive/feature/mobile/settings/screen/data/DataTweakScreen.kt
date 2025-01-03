package com.flixclusive.feature.mobile.settings.screen.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import com.flixclusive.model.datastore.user.DataPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal class DataTweakScreen(
    private val viewModel: SettingsViewModel,
) : BaseTweakScreen<DataPreferences> {
    override val key = UserPreferences.DATA_PREFS_KEY
    override val preferencesAsState: StateFlow<DataPreferences>
        = viewModel.getUserPrefsAsState<DataPreferences>(key)
    override val onUpdatePreferences: suspend (suspend (DataPreferences) -> DataPreferences) -> Boolean
        = { viewModel.updateUserPrefs(key, it) }

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
    override fun getTweaks(): List<Tweak> {
        val dataPreferences by preferencesAsState.collectAsStateWithLifecycle()

        return listOf(
            TweakUI.SwitchTweak(
                value = remember { mutableStateOf(dataPreferences.isIncognito) },
                title = stringResource(LocaleR.string.incognito),
                description = stringResource(LocaleR.string.incognito_content_desc),
                onTweaked = {
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(isIncognito = it)
                    }
                }
            ),
            getSearchTweaks(dataPreferences)
        )
    }

    @Composable
    private fun getSearchTweaks(
        dataPreferences: DataPreferences
    ): TweakGroup {
        val searchHistoryCount by viewModel.searchHistoryCount.collectAsStateWithLifecycle()

        return TweakGroup(
            title = stringResource(LocaleR.string.search),
            tweaks = persistentListOf(
                TweakUI.ClickableTweak(
                    title = stringResource(LocaleR.string.clear_search_history),
                    enabled = searchHistoryCount > 0,
                    description = stringResource(LocaleR.string.search_history_item_count_format, searchHistoryCount),
                    onClick = viewModel::clearSearchHistory,
                ),
            )
        )
    }
}