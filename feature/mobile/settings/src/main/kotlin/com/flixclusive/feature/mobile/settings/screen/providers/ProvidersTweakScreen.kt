package com.flixclusive.feature.mobile.settings.screen.providers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import com.flixclusive.feature.mobile.settings.util.LocalSettingsNavigator
import com.flixclusive.model.datastore.user.ProviderPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal class ProvidersTweakScreen(
    private val viewModel: SettingsViewModel,
) : BaseTweakScreen<ProviderPreferences> {
    override val key = UserPreferences.PROVIDER_PREFS_KEY
    override val preferencesAsState: StateFlow<ProviderPreferences> =
        viewModel.getUserPrefsAsState<ProviderPreferences>(key)
    override val onUpdatePreferences: suspend (suspend (ProviderPreferences) -> ProviderPreferences) -> Boolean =
        { viewModel.updateUserPrefs(key, it) }

    @Composable
    override fun getTitle(): String = stringResource(LocaleR.string.providers)

    @Composable
    override fun getIconPainter(): Painter = painterResource(UiCommonR.drawable.provider_logo)

    @Composable
    override fun getDescription(): String = stringResource(LocaleR.string.providers_settings_content_desc)

    @Composable
    override fun getTweaks(): List<Tweak> {
        val navigator = LocalSettingsNavigator.current!!
        val providerPreferences by preferencesAsState.collectAsStateWithLifecycle()

        return listOf(
            TweakUI.ClickableTweak(
                title = stringResource(LocaleR.string.manage_providers),
                description = stringResource(LocaleR.string.providers_button_settings_description),
                iconId = UiCommonR.drawable.provider_logo,
                onClick = navigator::openProvidersScreen,
            ),
            TweakUI.Divider,
            getGeneralTweaks(providerPreferences),
            getDataTweaks(providerPreferences),
        )
    }

    @Composable
    private fun getGeneralTweaks(providerPreferences: ProviderPreferences): TweakGroup =
        TweakGroup(
            title = stringResource(LocaleR.string.general),
            tweaks =
                persistentListOf(
                    TweakUI.SwitchTweak(
                        value = remember { mutableStateOf(providerPreferences.isAutoUpdateEnabled) },
                        title = stringResource(LocaleR.string.auto_update_providers),
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(isAutoUpdateEnabled = it)
                            }
                        },
                    ),
                    TweakUI.SwitchTweak(
                        value = remember { mutableStateOf(providerPreferences.shouldWarnBeforeInstall) },
                        title = stringResource(LocaleR.string.warn_on_unsafe_install),
                        description = stringResource(LocaleR.string.warn_on_unsafe_install_description),
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(shouldWarnBeforeInstall = it)
                            }
                        },
                    ),
                ),
        )

    @Composable
    private fun getDataTweaks(providerPreferences: ProviderPreferences): TweakGroup {
        val context = LocalContext.current
        val clearCachedLinksLabel = stringResource(LocaleR.string.clear_cached_links)
        val deleteProvidersLabel = stringResource(LocaleR.string.delete_providers)
        val deleteRepositoriesLabel = stringResource(LocaleR.string.delete_repositories)
        val warningLabel = stringResource(LocaleR.string.warning)

        val formatWarningMessage = fun (action: String): String = context.getString(LocaleR.string.action_warning_format_message, action)

        val formatWarningCountDescription = fun (items: Int): String = context.getString(LocaleR.string.warn_delete_items_format, items)

        return TweakGroup(
            title = stringResource(LocaleR.string.data),
            tweaks =
                persistentListOf(
                    TweakUI.DialogTweak(
                        title = clearCachedLinksLabel,
                        dialogTitle = warningLabel,
                        description = stringResource(LocaleR.string.cached_links_description_format, viewModel.cachedLinksSize),
                        dialogMessage = formatWarningMessage(clearCachedLinksLabel),
                        onConfirm = viewModel::clearCacheLinks,
                    ),
                    TweakUI.DialogTweak(
                        title = deleteProvidersLabel,
                        iconId = UiCommonR.drawable.warning_outline,
                        enabled = providerPreferences.providers.isNotEmpty(),
                        description = formatWarningCountDescription(providerPreferences.providers.size),
                        dialogTitle = warningLabel,
                        dialogMessage = formatWarningMessage(deleteProvidersLabel),
                        onConfirm = viewModel::deleteProviders,
                    ),
                    TweakUI.DialogTweak(
                        title = deleteRepositoriesLabel,
                        iconId = UiCommonR.drawable.warning_outline,
                        enabled = providerPreferences.repositories.isNotEmpty(),
                        description = formatWarningCountDescription(providerPreferences.repositories.size),
                        dialogTitle = warningLabel,
                        dialogMessage = formatWarningMessage(deleteRepositoriesLabel),
                        onConfirm = viewModel::deleteRepositories,
                    ),
                ),
        )
    }
}
