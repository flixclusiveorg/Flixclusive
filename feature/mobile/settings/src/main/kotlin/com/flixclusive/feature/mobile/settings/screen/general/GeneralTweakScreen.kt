package com.flixclusive.feature.mobile.settings.screen.general

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakScaffold
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalAppSettings
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.getCurrentSettingsViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal class GeneralTweakScreen(
    private val showPrereleaseWarning: MutableState<Boolean>
) : BaseTweakScreen {

    @Composable
    override fun getTitle(): String
        = stringResource(LocaleR.string.general)

    @Composable
    override fun getDescription(): String
        = stringResource(LocaleR.string.general_settings_content_desc)

    @Composable
    override fun getIconPainter(): Painter
        = painterResource(UiCommonR.drawable.general_settings)

    @Composable
    override fun getTweaks(): List<Tweak> {
        return getUpdatesTweaks()
    }

    @Composable
    override fun Content() {
        val appSettings = LocalAppSettings.current
        val viewModel = getCurrentSettingsViewModel()
        val onTweaked = viewModel::onChangeAppSettings

        if (showPrereleaseWarning.value) {
            PreReleaseWarningDialog(
                onConfirm = {
                    onTweaked(appSettings.copy(isUsingPrereleaseUpdates = true))
                    showPrereleaseWarning.value = false
                },
                onDismiss = { showPrereleaseWarning.value = false }
            )
        }

        TweakScaffold(
            title = getTitle(),
            description = getDescription(),
            tweaksProvider = { getTweaks() }
        )
    }

    @Composable
    private fun getUpdatesTweaks(): ImmutableList<TweakUI<out Any>> {
        val appSettings = LocalAppSettings.current
        val viewModel = getCurrentSettingsViewModel()
        val onTweaked = viewModel::onChangeAppSettings

        val usePreReleaseUpdates = remember(appSettings.isUsingPrereleaseUpdates) {
            mutableStateOf(appSettings.isUsingPrereleaseUpdates)
        }

        return persistentListOf(
            TweakUI.SwitchTweak(
                value = remember { mutableStateOf(appSettings.isUsingAutoUpdateAppFeature) },
                title = stringResource(LocaleR.string.notify_about_new_app_updates),
                onTweaked = {
                    onTweaked(appSettings.copy(isUsingAutoUpdateAppFeature = it))
                    true
                }
            ),
            TweakUI.SwitchTweak(
                value = usePreReleaseUpdates,
                title = stringResource(LocaleR.string.sign_up_prerelease),
                description = stringResource(LocaleR.string.signup_prerelease_updates_desc),
                onTweaked = { state ->
                    return@SwitchTweak if (state && !showPrereleaseWarning.value) {
                        showPrereleaseWarning.value = true
                        false
                    } else {
                        onTweaked(appSettings.copy(isUsingPrereleaseUpdates = false))
                        true
                    }
                }
            ),
            TweakUI.SwitchTweak(
                value = remember { mutableStateOf(appSettings.isSendingCrashLogsAutomatically) },
                title = stringResource(LocaleR.string.automatic_crash_report),
                description = stringResource(LocaleR.string.automatic_crash_report_label),
                onTweaked = {
                    onTweaked(appSettings.copy(isSendingCrashLogsAutomatically = it))
                    true
                }
            ),
        )
    }
}