package com.flixclusive.feature.mobile.settings.screen.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withMainContext
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import com.flixclusive.model.datastore.system.SystemPreferences
import com.flixclusive.model.datastore.user.network.DoHPreference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.flixclusive.core.strings.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal class SystemTweakScreen(
    viewModel: SettingsViewModel,
) : BaseTweakScreen<SystemPreferences> {
    override val key = stringPreferencesKey("system_preferences")
    override val preferencesAsState: StateFlow<SystemPreferences> = viewModel.systemPreferences
    override val onUpdatePreferences: suspend (suspend (SystemPreferences) -> SystemPreferences) -> Boolean =
        { viewModel.updateSystemPrefs(it) }

    @Composable
    override fun getTitle(): String = stringResource(LocaleR.string.system)

    @Composable
    override fun getDescription(): String = stringResource(LocaleR.string.system_settings_content_desc)

    @Composable
    override fun getIconPainter(): Painter = painterResource(UiCommonR.drawable.wrench)

    @Composable
    override fun getTweaks(): List<Tweak> {
        val systemPreferences by preferencesAsState.collectAsStateWithLifecycle()

        return getUpdatesTweaks({ systemPreferences }) +
            listOf(
                getNetworkTweaks({ systemPreferences }),
            )
    }

    @Composable
    private fun getUpdatesTweaks(systemPreferencesProvider: () -> SystemPreferences): ImmutableList<TweakUI<out Any>> {
        val context = LocalContext.current
        val usePreReleaseUpdates =
            remember { mutableStateOf(systemPreferencesProvider().isUsingPrereleaseUpdates) }
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
                value = remember { mutableStateOf(systemPreferencesProvider().isUsingAutoUpdateAppFeature) },
                title = context.getString(LocaleR.string.notify_about_new_app_updates),
                onTweaked = {
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(isUsingAutoUpdateAppFeature = true)
                    }
                    true
                },
            ),
            TweakUI.SwitchTweak(
                value = usePreReleaseUpdates,
                title = context.getString(LocaleR.string.sign_up_prerelease),
                descriptionProvider = { context.getString(LocaleR.string.signup_prerelease_updates_desc) },
                onTweaked = { state ->
                    return@SwitchTweak if (state && !showPrereleaseWarning.value) {
                        showPrereleaseWarning.value = true
                        false
                    } else {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(isUsingPrereleaseUpdates = false)
                        }
                        true
                    }
                },
            ),
            TweakUI.SwitchTweak(
                value = remember { mutableStateOf(systemPreferencesProvider().isSendingCrashLogsAutomatically) },
                title = context.getString(LocaleR.string.automatic_crash_report),
                descriptionProvider = { context.getString(LocaleR.string.automatic_crash_report_label) },
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

        val userAgent = remember { mutableStateOf(systemPreferencesProvider().userAgent) }
        val currentDoH = remember { mutableStateOf(systemPreferencesProvider().dns) }
        val availableDoHServers =
            remember {
                DoHPreference.entries
                    .associateWith { it.name }
                    .toImmutableMap()
            }

        return TweakGroup(
            title = stringResource(LocaleR.string.network),
            tweaks =
                persistentListOf(
                    TweakUI.ListTweak(
                        title = stringResource(LocaleR.string.doh),
                        descriptionProvider = { context.getString(LocaleR.string.doh_content_desc) },
                        value = currentDoH,
                        options = availableDoHServers,
                        onTweaked = {
                            val success =
                                if (currentDoH.value != it) {
                                    onUpdatePreferences { oldValue ->
                                        oldValue.copy(dns = it)
                                    }
                                } else {
                                    false
                                }

                            withMainContext {
                                if (success) {
                                    val message =
                                        context.getString(LocaleR.string.restart_app_for_changes_message)
                                    context.showToast(message)
                                }
                            }

                            success
                        },
                    ),
                    // TODO: Make this a CustomContentTweak
                    TweakUI.TextFieldTweak(
                        value = userAgent,
                        title = context.getString(LocaleR.string.default_user_agent),
                        descriptionProvider = { context.getString(LocaleR.string.default_user_agent_description) },
                        onTweaked = {
                            val success =
                                if (userAgent.value != it) {
                                    onUpdatePreferences { oldValue ->
                                        oldValue.copy(userAgent = it)
                                    }
                                } else {
                                    false
                                }

                            withMainContext {
                                if (success) {
                                    val message =
                                        context.getString(LocaleR.string.restart_app_for_changes_message)
                                    context.showToast(message)
                                }
                            }

                            success
                        },
                    ),
                ),
        )
    }
}
