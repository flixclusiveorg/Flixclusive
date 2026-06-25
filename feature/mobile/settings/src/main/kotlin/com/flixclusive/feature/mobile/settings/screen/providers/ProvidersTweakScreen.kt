package com.flixclusive.feature.mobile.settings.screen.providers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.database.entity.provider.InstalledRepository
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakScaffold
import com.flixclusive.feature.mobile.settings.TweakUI
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

interface NavigatorProvidersTweakScreen : NavigateBack {
    fun navigateToProviderManagerScreen()

    fun navigateToRepositoryManagerScreen()
}

@Destination<ExternalModuleGraph>
@Composable
internal fun ProvidersTweakScreen(
    navigator: NavigatorProvidersTweakScreen,
    viewModel: ProvidersTweakViewModel = hiltViewModel()
) {
    val resources = LocalResources.current

    val providerPreferences by viewModel.preferences.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val repositories by viewModel.repositories.collectAsStateWithLifecycle()

    TweakScaffold(
        title = stringResource(LocaleR.string.providers),
        description = stringResource(LocaleR.string.providers_settings_content_desc),
        navigateBack = navigator::navigateBack,
        tweaksProvider = {
            listOf(
                TweakUI.ClickableTweak(
                    title = stringResource(LocaleR.string.manage_providers),
                    description = { resources.getString(LocaleR.string.providers_button_settings_description) },
                    iconId = UiCommonR.drawable.provider_logo,
                    onClick = navigator::navigateToProviderManagerScreen,
                ),
                TweakUI.Divider(),
                TweakUI.ClickableTweak(
                    title = stringResource(LocaleR.string.manage_repositories),
                    description = { resources.getString(LocaleR.string.repositories_button_settings_description) },
                    iconId = UiCommonR.drawable.repository,
                    onClick = navigator::navigateToRepositoryManagerScreen,
                ),
                TweakUI.Divider(),
                getGeneralTweaks(
                    providerPreferences = { providerPreferences },
                    onUpdatePreferences = viewModel::updateUserPrefs,
                ),
                getTestingTweaks(
                    providerPreferences = { providerPreferences },
                    onUpdatePreferences = viewModel::updateUserPrefs,
                ),
                getDataTweaks(
                    repositories = { repositories },
                    providers = { providers },
                    deleteProviders = viewModel::deleteProviders,
                    deleteRepositories = viewModel::deleteRepositories,
                ),
            )
        },
    )
}

@Composable
private fun getGeneralTweaks(
    providerPreferences: () -> ProviderPreferences,
    onUpdatePreferences: (suspend (oldValue: ProviderPreferences) -> ProviderPreferences) -> Unit,
): TweakGroup {
    val resources = LocalResources.current

    return TweakGroup(
        title = stringResource(LocaleR.string.general),
        tweaks = persistentListOf(
            TweakUI.SwitchTweak(
                value = { providerPreferences().isAutoUpdateEnabled },
                title = stringResource(LocaleR.string.auto_update_providers),
                onTweaked = {
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(isAutoUpdateEnabled = it)
                    }
                },
            ),
            TweakUI.SwitchTweak(
                value = { providerPreferences().shouldWarnBeforeInstall },
                title = stringResource(LocaleR.string.warn_on_unsafe_install),
                description = {
                    resources.getString(LocaleR.string.warn_on_unsafe_install_description)
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
private fun getTestingTweaks(
    providerPreferences: () -> ProviderPreferences,
    onUpdatePreferences: (suspend (oldValue: ProviderPreferences) -> ProviderPreferences) -> Unit,
): TweakGroup {
    val resources = LocalResources.current
    return TweakGroup(
        title = stringResource(LocaleR.string.test),
        tweaks = persistentListOf(
            TweakUI.SwitchTweak(
                value = { providerPreferences().shouldAddDebugPrefix },
                title = stringResource(LocaleR.string.add_debug_prefix),
                description = {
                    resources.getString(LocaleR.string.add_debug_prefix_settings_description)
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
private fun getDataTweaks(
    repositories: () -> List<InstalledRepository>,
    providers: () -> List<ProviderResponseWrapper>,
    deleteProviders: () -> Unit,
    deleteRepositories: () -> Unit,
): TweakGroup {
    val resources = LocalResources.current
    val deleteProvidersLabel = stringResource(LocaleR.string.delete_providers)
    val deleteRepositoriesLabel = stringResource(LocaleR.string.delete_repositories)
    val warningLabel = stringResource(LocaleR.string.warning)

    val formatWarningMessage = fun(action: String): String =
        resources.getString(
            LocaleR.string.action_warning_format_message,
            action,
        )

    val formatWarningCountDescription = fun(items: Int): String =
        resources.getString(
            LocaleR.string.warn_delete_items_format,
            items,
        )

    return TweakGroup(
        title = stringResource(LocaleR.string.data),
        tweaks = persistentListOf(
            TweakUI.DialogTweak(
                title = deleteProvidersLabel,
                iconId = UiCommonR.drawable.warning_outline,
                enabledProvider = { providers().isNotEmpty() },
                description = { formatWarningCountDescription(providers().size) },
                dialogTitle = warningLabel,
                dialogMessage = formatWarningMessage(deleteProvidersLabel),
                onConfirm = deleteProviders,
            ),
            TweakUI.DialogTweak(
                title = deleteRepositoriesLabel,
                iconId = UiCommonR.drawable.warning_outline,
                enabledProvider = { repositories().isNotEmpty() },
                description = {
                    formatWarningCountDescription(repositories().size)
                },
                dialogTitle = warningLabel,
                dialogMessage = formatWarningMessage(deleteRepositoriesLabel),
                onConfirm = deleteRepositories,
            ),
        ),
    )
}
