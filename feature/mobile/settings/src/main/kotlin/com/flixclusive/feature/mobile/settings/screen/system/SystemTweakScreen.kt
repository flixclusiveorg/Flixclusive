package com.flixclusive.feature.mobile.settings.screen.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.network.DoHPreference
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakScaffold
import com.flixclusive.feature.mobile.settings.TweakUI
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import com.flixclusive.core.strings.R as LocaleR

@Destination<ExternalModuleGraph>
@Composable
internal fun SystemTweakScreen(
    navigator: NavigateBack,
    viewModel: SystemTweakViewModel = hiltViewModel()
) {
    val systemPreferences by viewModel.preferences.collectAsStateWithLifecycle()

    TweakScaffold(
        title = stringResource(LocaleR.string.system),
        description = stringResource(LocaleR.string.system_settings_content_desc),
        navigateBack = navigator::navigateBack,
        tweaksProvider = {
            getUpdatesTweaks(
                systemPreferences = { systemPreferences },
                onUpdatePreferences = viewModel::updateSystemPrefs,
            ) + listOf(
                getNetworkTweaks(
                    systemPreferences = { systemPreferences },
                    onUpdatePreferences = viewModel::updateSystemPrefs,
                )
            )
        }
    )
}

@Composable
private fun getUpdatesTweaks(
    systemPreferences: () -> SystemPreferences,
    onUpdatePreferences: (suspend (SystemPreferences) -> SystemPreferences) -> Unit,
): ImmutableList<TweakUI<out Any>> {
    val resources = LocalResources.current
    val uriLauncher = LocalUriHandler.current

    return persistentListOf(
        TweakUI.ClickableTweak(
            title = resources.getString(LocaleR.string.sign_up_prerelease),
            description = { resources.getString(LocaleR.string.signup_prerelease_updates_desc) },
            onClick = {
                uriLauncher.openUri("https://github.com/flixclusive/preview-builds/releases/latest")
            }
        ),
        TweakUI.SwitchTweak(
            value = { systemPreferences().isUsingAutoUpdateAppFeature },
            title = resources.getString(LocaleR.string.notify_about_new_app_updates),
            onTweaked = { state ->
                onUpdatePreferences { oldValue ->
                    oldValue.copy(isUsingAutoUpdateAppFeature = state)
                }
            },
        ),
        TweakUI.SwitchTweak(
            value = { systemPreferences().isSendingCrashLogsAutomatically },
            title = resources.getString(LocaleR.string.automatic_crash_report),
            description = { resources.getString(LocaleR.string.automatic_crash_report_label) },
            onTweaked = { state ->
                onUpdatePreferences { oldValue ->
                    oldValue.copy(isSendingCrashLogsAutomatically = state)
                }
            },
        ),
    )
}

@Composable
private fun getNetworkTweaks(
    systemPreferences: () -> SystemPreferences,
    onUpdatePreferences: (suspend (SystemPreferences) -> SystemPreferences) -> Unit,
): TweakGroup {
    val context = LocalContext.current
    val resources = LocalResources.current

    val availableDoHServers = remember {
        DoHPreference.entries
            .associateWith { it.name }
            .toImmutableMap()
    }

    return TweakGroup(
        title = stringResource(LocaleR.string.network),
        tweaks = persistentListOf(
            TweakUI.ListTweak(
                title = stringResource(LocaleR.string.doh),
                description = { resources.getString(LocaleR.string.doh_content_desc) },
                value = { systemPreferences().dns },
                options = availableDoHServers,
                onTweaked = {
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(dns = it)
                    }

                    val message = resources.getString(LocaleR.string.restart_app_for_changes_message)
                    context.showToast(message)
                },
            ),
            // TODO: Make this a CustomContentTweak
            TweakUI.TextFieldTweak(
                value = { systemPreferences().userAgent },
                title = resources.getString(LocaleR.string.default_user_agent),
                description = { resources.getString(LocaleR.string.default_user_agent_description) },
                onTweaked = {
                    if (systemPreferences().userAgent != it) {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(userAgent = it)
                        }

                        val message = resources.getString(LocaleR.string.restart_app_for_changes_message)
                        context.showToast(message)
                    }
                },
            ),
        ),
    )
}
