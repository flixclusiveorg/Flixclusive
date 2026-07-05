package com.flixclusive.feature.mobile.settings.screen.data

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakScaffold
import com.flixclusive.feature.mobile.settings.TweakUI
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.strings.R as LocaleR

interface NavigatorDataTweakScreen : NavigateBack {
    fun navigateToMediaLinkCardsTweakScreen()
}

@Destination<ExternalModuleGraph>
@Composable
internal fun DataTweakScreen(
    navigator: NavigatorDataTweakScreen,
    viewModel: BackupTweakViewModel = hiltViewModel()
) {
    val dataPreferences by viewModel.preferences.collectAsStateWithLifecycle()
    val systemPreferences by viewModel.systemPreferences.collectAsStateWithLifecycle()
    val cachedLinksCount by viewModel.cachedLinksCount.collectAsStateWithLifecycle()
    val searchHistoryCount by viewModel.searchHistoryCount.collectAsStateWithLifecycle()

    val resources = LocalResources.current

    TweakScaffold(
        title = stringResource(LocaleR.string.data),
        description = stringResource(LocaleR.string.data_settings_content_desc),
        navigateBack = navigator::navigateBack,
        tweaksProvider = {
            listOf(
                getSearchTweaks(
                    searchHistoryCount = searchHistoryCount,
                    clearSearchHistory = viewModel::clearSearchHistory,
                ),
                getCachedLinksTweaks(
                    dataPreferences = { dataPreferences },
                    cacheSize = cachedLinksCount,
                    onOpenMediaLinkCardsTweakScreen = navigator::navigateToMediaLinkCardsTweakScreen,
                    clearCacheLinks = viewModel::clearCacheLinks,
                    onUpdatePreferences = viewModel::updateUserPrefs,
                ),
                backupTweakGroup(
                    dataPreferences = { dataPreferences },
                    systemPreferences = { systemPreferences },
                    onUpdatePreferences = viewModel::updateUserPrefs,
                    onUpdateSystemPreferences = viewModel::updateSystemPrefs,
                    createBackup = viewModel::createBackup,
                    restoreBackup = viewModel::restoreBackup,
                ),
            )
        }
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun getCachedLinksTweaks(
    dataPreferences: () -> DataPreferences,
    cacheSize: Int,
    clearCacheLinks: () -> Unit,
    onOpenMediaLinkCardsTweakScreen: () -> Unit,
    onUpdatePreferences: (suspend (oldValue: DataPreferences) -> DataPreferences) -> Unit,
): TweakGroup {
    val resources = LocalResources.current

    return TweakGroup(
        title = stringResource(LocaleR.string.label_cached_links),
        tweaks = persistentListOf(
            TweakUI.ClickableTweak(
                title = stringResource(LocaleR.string.label_manage_cached_links),
                description = {
                    resources.getString(LocaleR.string.desc_manage_cached_links_content_desc)
                },
                onClick = onOpenMediaLinkCardsTweakScreen,
            ),
            TweakUI.ClickableTweak(
                title = stringResource(LocaleR.string.clear_cached_links),
                enabledProvider = { cacheSize > 0 },
                onClick = clearCacheLinks,
                description = {
                    resources.getString(
                        LocaleR.string.cached_links_description_format,
                        cacheSize,
                    )
                },
            ),
            TweakUI.SliderTweak(
                title = stringResource(LocaleR.string.dead_link_retention_days_title),
                description = {
                    resources.getString(
                        LocaleR.string.dead_link_retention_days_desc,
                        dataPreferences().deadLinkRetentionDays,
                    )
                },
                value = { dataPreferences().deadLinkRetentionDays.toFloat() },
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
private fun getSearchTweaks(
    searchHistoryCount: Int,
    clearSearchHistory: () -> Unit,
): TweakGroup {
    val resources = LocalResources.current

    return TweakGroup(
        title = stringResource(LocaleR.string.search),
        tweaks = persistentListOf(
            TweakUI.ClickableTweak(
                title = stringResource(LocaleR.string.clear_search_history),
                enabledProvider = { searchHistoryCount > 0 },
                onClick = clearSearchHistory,
                description = {
                    resources.getString(
                        LocaleR.string.search_history_item_count_format,
                        searchHistoryCount,
                    )
                },
            ),
        ),
    )
}
