package com.flixclusive.feature.mobile.settings.screen.data

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.flixclusive.feature.mobile.settings.screen.links.MediaLinkCardsTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import com.flixclusive.feature.mobile.settings.util.LocalScaffoldNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    override fun getTitle(): String = stringResource(LocaleR.string.data)

    @Composable
    override fun getIconPainter(): Painter = painterResource(UiCommonR.drawable.database_icon_thin)

    @Composable
    override fun getDescription(): String = stringResource(LocaleR.string.data_settings_content_desc)

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
            getSearchTweaks(),
            getCachedLinksTweaks(dataPreferences = dataPreferences),
            backupTweakGroup(
                dataPreferences = { dataPreferences },
                systemPreferences = { systemPreferences },
                onUpdatePreferences = ::onUpdatePreferences,
                onUpdateSystemPreferences = viewModel::updateSystemPrefs,
                createBackup = viewModel::createBackup,
                restoreBackup = viewModel::restoreBackup,
            ),
        )
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Composable
    private fun getCachedLinksTweaks(dataPreferences: DataPreferences): TweakGroup {
        val resources = LocalResources.current
        val navigator = LocalScaffoldNavigator.current
        val cachedLinksCount by viewModel.cachedLinksCount.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

        return TweakGroup(
            title = stringResource(LocaleR.string.label_cached_links),
            tweaks = persistentListOf(
                TweakUI.ClickableTweak(
                    title = stringResource(LocaleR.string.label_manage_cached_links),
                    description = {
                        resources.getString(LocaleR.string.desc_manage_cached_links_content_desc)
                    },
                    onClick = {
                        scope.launch {
                            navigator?.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = MediaLinkCardsTweakScreen.key.name,
                            )
                        }
                    },
                ),
                TweakUI.ClickableTweak(
                    title = stringResource(LocaleR.string.clear_cached_links),
                    enabledProvider = { cachedLinksCount > 0 },
                    onClick = viewModel::clearCacheLinks,
                    description = {
                        resources.getString(
                            LocaleR.string.cached_links_description_format,
                            cachedLinksCount,
                        )
                    },
                ),
                TweakUI.SliderTweak(
                    title = stringResource(LocaleR.string.dead_link_retention_days_title),
                    description = {
                        resources.getString(
                            LocaleR.string.dead_link_retention_days_desc,
                            dataPreferences.deadLinkRetentionDays,
                        )
                    },
                    value = { dataPreferences.deadLinkRetentionDays.toFloat() },
                    range = 0f..30f,
                    steps = 29,
                    onTweaked = { days ->
                        onUpdatePreferences { it.copy(deadLinkRetentionDays = days.toInt()) }
                    },
                ),
            ),
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
