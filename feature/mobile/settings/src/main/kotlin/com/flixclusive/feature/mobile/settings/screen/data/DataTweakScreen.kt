package com.flixclusive.feature.mobile.settings.screen.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

internal class DataTweakScreen(
    private val viewModel: SettingsViewModel,
) : BaseTweakScreen<DataPreferences> {
    override val key = UserPreferences.DATA_PREFS_KEY
    override val preferencesAsState: StateFlow<DataPreferences> = viewModel.getUserPrefsAsState<DataPreferences>(key)
    override fun onUpdatePreferences(transform: suspend (t: DataPreferences) -> DataPreferences) {
        viewModel.updateUserPrefs(key, transform)
    }

    @Composable
    override fun getTitle(): String = stringResource(LocaleR.string.data_and_backup)

    @Composable
    override fun getIconPainter(): Painter = painterResource(UiCommonR.drawable.database_icon_thin)

    @Composable
    override fun getDescription(): String = stringResource(LocaleR.string.data_and_backup_settings_content_desc)

    @Composable
    override fun getTweaks(): List<Tweak> {
        val dataPreferences by preferencesAsState.collectAsStateWithLifecycle()
        val systemPreferences by viewModel.systemPreferences.collectAsStateWithLifecycle()
        val resources = LocalResources.current

        return listOf(
            TweakUI.SwitchTweak(
                value = { dataPreferences.isIncognito },
                title = stringResource(LocaleR.string.incognito),
                description = { resources.getString(LocaleR.string.incognito_content_desc) },
                onTweaked = {
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(isIncognito = it)
                    }
                },
            ),
            backupTweakGroup(
                dataPreferences = { dataPreferences },
                systemPreferences = { systemPreferences },
                onUpdatePreferences = ::onUpdatePreferences,
                onUpdateSystemPreferences = viewModel::updateSystemPrefs,
                createBackup = viewModel::createBackup,
                restoreBackup = viewModel::restoreBackup,
            ),
            getSearchTweaks(),
        )
    }

    @Composable
    private fun getSearchTweaks(): TweakGroup {
        val resources = LocalResources.current
        val searchHistoryCount = viewModel.searchHistoryCount.collectAsStateWithLifecycle()

        return TweakGroup(
            title = stringResource(LocaleR.string.search),
            tweaks = persistentListOf(
                TweakUI.ClickableTweak(
                    title = stringResource(LocaleR.string.clear_search_history),
                    enabledProvider = { searchHistoryCount.value > 0 },
                    onClick = viewModel::clearSearchHistory,
                    description = {
                        resources.getString(
                            LocaleR.string.search_history_item_count_format,
                            searchHistoryCount.value,
                        )
                    },
                ),
            ),
        )
    }
}
