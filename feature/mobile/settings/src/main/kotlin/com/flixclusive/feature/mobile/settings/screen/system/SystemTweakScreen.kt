package com.flixclusive.feature.mobile.settings.screen.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.network.DoHPreference
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

internal class SystemTweakScreen(
    private val viewModel: SettingsViewModel,
) : BaseTweakScreen<SystemPreferences> {
    override val key = stringPreferencesKey("system_preferences")
    override val preferencesAsState: StateFlow<SystemPreferences> = viewModel.systemPreferences

    override fun onUpdatePreferences(transform: suspend (SystemPreferences) -> SystemPreferences) {
        viewModel.updateSystemPrefs(transform)
    }

    @Composable
    override fun getTitle(): String = stringResource(LocaleR.string.system)

    @Composable
    override fun getDescription(): String = stringResource(LocaleR.string.system_settings_content_desc)

    @Composable
    override fun getIconPainter(): Painter = painterResource(UiCommonR.drawable.wrench)

    @Composable
    override fun getTweaks(): List<Tweak> {
        val systemPreferences by preferencesAsState.collectAsStateWithLifecycle()

        return getUpdatesTweaks { systemPreferences } +
            listOf(getNetworkTweaks { systemPreferences })
    }

    @Composable
    private fun getUpdatesTweaks(systemPreferencesProvider: () -> SystemPreferences): ImmutableList<TweakUI<out Any>> {
        val resources = LocalResources.current
        val showPrereleaseWarning = remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()

        if (showPrereleaseWarning.value) {
            PreReleaseWarningDialog(
                onDismiss = { showPrereleaseWarning.value = false },
                onConfirm = {
                    scope.launch {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(isUsingPrereleaseUpdates = true)
                        }
                        showPrereleaseWarning.value = false
                    }
                },
            )
        }

        return persistentListOf(
            TweakUI.SwitchTweak(
                value = { systemPreferencesProvider().isUsingAutoUpdateAppFeature },
                title = resources.getString(LocaleR.string.notify_about_new_app_updates),
                onTweaked = {
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(isUsingAutoUpdateAppFeature = true)
                    }
                },
            ),
            TweakUI.SwitchTweak(
                value = { systemPreferencesProvider().isUsingPrereleaseUpdates },
                title = resources.getString(LocaleR.string.sign_up_prerelease),
                description = { resources.getString(LocaleR.string.signup_prerelease_updates_desc) },
                onTweaked = { state ->
                    if (state && !showPrereleaseWarning.value) {
                        showPrereleaseWarning.value = true
                    } else {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(isUsingPrereleaseUpdates = false)
                        }
                    }
                },
            ),
            TweakUI.SwitchTweak(
                value = { systemPreferencesProvider().isSendingCrashLogsAutomatically },
                title = resources.getString(LocaleR.string.automatic_crash_report),
                description = { resources.getString(LocaleR.string.automatic_crash_report_label) },
                onTweaked = {
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(isSendingCrashLogsAutomatically = it)
                    }
                },
            ),
        )
    }

    @Composable
    private fun getNetworkTweaks(systemPreferencesProvider: () -> SystemPreferences): TweakGroup {
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
                    value = { systemPreferencesProvider().dns },
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
                    value = { systemPreferencesProvider().userAgent },
                    title = resources.getString(LocaleR.string.default_user_agent),
                    description = { resources.getString(LocaleR.string.default_user_agent_description) },
                    onTweaked = {
                        if (systemPreferencesProvider().userAgent != it) {
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
}
