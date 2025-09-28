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
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import com.flixclusive.feature.mobile.settings.util.LocalSettingsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

internal class ProvidersTweakScreen(
    private val viewModel: SettingsViewModel,
) : BaseTweakScreen<ProviderPreferences> {
    override val key = UserPreferences.PROVIDER_PREFS_KEY
    override val preferencesAsState: StateFlow<ProviderPreferences> =
        viewModel.getUserPrefsAsState<ProviderPreferences>(key)

    override suspend fun onUpdatePreferences(
        transform: suspend (t: ProviderPreferences) -> ProviderPreferences,
    ): Boolean {
        return viewModel.updateUserPrefs(key, transform)
    }

    @Composable
    override fun getTitle(): String = stringResource(LocaleR.string.providers)

    @Composable
    override fun getIconPainter(): Painter = painterResource(UiCommonR.drawable.provider_logo)

    @Composable
    override fun getDescription(): String = stringResource(LocaleR.string.providers_settings_content_desc)

    @Composable
    override fun getTweaks(): List<Tweak> {
        val context = LocalContext.current
        val navigator = LocalSettingsNavigator.current!!
        val providerPreferences = preferencesAsState.collectAsStateWithLifecycle()

        return listOf(
            TweakUI.ClickableTweak(
                title = stringResource(LocaleR.string.manage_providers),
                descriptionProvider = { context.getString(LocaleR.string.providers_button_settings_description) },
                iconId = UiCommonR.drawable.provider_logo,
                onClick = navigator::openProviderManagerScreen,
            ),
            TweakUI.ClickableTweak(
                title = stringResource(LocaleR.string.test_providers),
                enabledProvider = { providerPreferences.value.providers.isNotEmpty() },
                descriptionProvider = { context.getString(LocaleR.string.test_providers_button_settings_description) },
                iconId = UiCommonR.drawable.test,
                onClick = { navigator.testProviders(arrayListOf()) },
            ),
            TweakUI.Divider(),
            TweakUI.ClickableTweak(
                title = stringResource(LocaleR.string.manage_repositories),
                descriptionProvider = { context.getString(LocaleR.string.repositories_button_settings_description) },
                iconId = UiCommonR.drawable.repository,
                onClick = navigator::openRepositoryManagerScreen,
            ),
            TweakUI.Divider(),
            getGeneralTweaks { providerPreferences.value },
            getTestingTweaks { providerPreferences.value },
            getDataTweaks { providerPreferences.value },
        )
    }

    @Composable
    private fun getGeneralTweaks(providerPreferences: () -> ProviderPreferences): TweakGroup {
        val context = LocalContext.current

        return TweakGroup(
            title = stringResource(LocaleR.string.general),
            tweaks =
                persistentListOf(
                    TweakUI.SwitchTweak(
                        value = remember { mutableStateOf(providerPreferences().isAutoUpdateEnabled) },
                        title = stringResource(LocaleR.string.auto_update_providers),
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(isAutoUpdateEnabled = it)
                            }
                        },
                    ),
                    TweakUI.SwitchTweak(
                        value = remember { mutableStateOf(providerPreferences().shouldWarnBeforeInstall) },
                        title = stringResource(LocaleR.string.warn_on_unsafe_install),
                        descriptionProvider = {
                            context.getString(
                                LocaleR.string.warn_on_unsafe_install_description,
                            )
                        },
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(shouldWarnBeforeInstall = it)
                            }
                        },
                    ),
                ),
        )
    }

    @Composable
    private fun getTestingTweaks(providerPreferences: () -> ProviderPreferences): TweakGroup {
        val context = LocalContext.current
        return TweakGroup(
            title = stringResource(LocaleR.string.test),
            tweaks =
                persistentListOf(
                    TweakUI.SwitchTweak(
                        value = remember { mutableStateOf(providerPreferences().shouldAddDebugPrefix) },
                        title = stringResource(LocaleR.string.add_debug_prefix),
                        descriptionProvider = {
                            context.getString(
                                LocaleR.string.add_debug_prefix_settings_description,
                            )
                        },
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(shouldAddDebugPrefix = it)
                            }
                        },
                    ),
                ),
        )
    }

    @Composable
    private fun getDataTweaks(providerPreferences: () -> ProviderPreferences): TweakGroup {
        val context = LocalContext.current
        val clearCachedLinksLabel = stringResource(LocaleR.string.clear_cached_links)
        val deleteProvidersLabel = stringResource(LocaleR.string.delete_providers)
        val deleteRepositoriesLabel = stringResource(LocaleR.string.delete_repositories)
        val warningLabel = stringResource(LocaleR.string.warning)

        val cacheSize by viewModel.cachedLinksSize.collectAsStateWithLifecycle()

        val formatWarningMessage = fun(action: String): String =
            context.getString(
                LocaleR.string.action_warning_format_message,
                action,
            )

        val formatWarningCountDescription = fun(items: Int): String =
            context.getString(
                LocaleR.string.warn_delete_items_format,
                items,
            )

        val providers = remember { mutableStateOf(providerPreferences().providers) }
        val onDeleteProviders = remember(viewModel) { viewModel::deleteProviders }

        val repositories = remember { mutableStateOf(providerPreferences().repositories) }
        val onDeleteRepositories = remember(viewModel) { viewModel::deleteRepositories }

        return TweakGroup(
            title = stringResource(LocaleR.string.data),
            tweaks =
                persistentListOf(
                    TweakUI.DialogTweak(
                        title = clearCachedLinksLabel,
                        dialogTitle = warningLabel,
                        enabledProvider = { cacheSize > 0 },
                        descriptionProvider = {
                            context.getString(
                                LocaleR.string.cached_links_description_format,
                                cacheSize,
                            )
                        },
                        dialogMessage = formatWarningMessage(clearCachedLinksLabel),
                        onConfirm = viewModel::clearCacheLinks,
                    ),
                    TweakUI.DialogTweak(
                        title = deleteProvidersLabel,
                        iconId = UiCommonR.drawable.warning_outline,
                        enabledProvider = { providers.value.isNotEmpty() },
                        descriptionProvider = { formatWarningCountDescription(providers.value.size) },
                        dialogTitle = warningLabel,
                        dialogMessage = formatWarningMessage(deleteProvidersLabel),
                        onConfirm = {
                            onDeleteProviders()
                            providers.value = emptyList()
                        },
                    ),
                    TweakUI.DialogTweak(
                        title = deleteRepositoriesLabel,
                        iconId = UiCommonR.drawable.warning_outline,
                        enabledProvider = { repositories.value.isNotEmpty() },
                        descriptionProvider = {
                            formatWarningCountDescription(repositories.value.size)
                        },
                        dialogTitle = warningLabel,
                        dialogMessage = formatWarningMessage(deleteRepositoriesLabel),
                        onConfirm = {
                            onDeleteRepositories()
                            repositories.value = emptyList()
                        },
                    ),
                ),
        )
    }
}
