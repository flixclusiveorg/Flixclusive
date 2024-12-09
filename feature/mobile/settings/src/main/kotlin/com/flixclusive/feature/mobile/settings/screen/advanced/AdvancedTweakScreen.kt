package com.flixclusive.feature.mobile.settings.screen.advanced

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.core.ui.common.util.showToast
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalAppSettings
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.rememberAppSettingsChanger
import com.flixclusive.model.datastore.network.DoHPreference
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal object AdvancedTweakScreen : BaseTweakScreen {
    @Composable
    override fun getTitle(): String
        = stringResource(LocaleR.string.advanced)

    @Composable
    override fun getIconPainter(): Painter
        = painterResource(UiCommonR.drawable.code)

    @Composable
    override fun getDescription(): String
        = stringResource(LocaleR.string.advanced_settings_content_desc)

    @Composable
    override fun getTweaks(): List<Tweak>
        = listOf(getNetworkTweaks())

    @Composable
    override fun Content() {
        TODO("Not yet implemented")
    }

    @Composable
    private fun getNetworkTweaks(): TweakGroup {
        val context = LocalContext.current

        val appSettings = LocalAppSettings.current
        val onTweaked by rememberAppSettingsChanger()

        val currentDoH = remember { mutableStateOf(appSettings.dns) }
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
                    description = stringResource(LocaleR.string.doh_content_desc),
                    value = currentDoH,
                    options = availableDoHServers,
                    onTweaked = {
                        if (currentDoH.value != it) {
                            onTweaked(appSettings.copy(dns = it))

                            val message = context.getString(LocaleR.string.restart_app_for_changes_message)
                            context.showToast(message)
                        }
                        true
                    }
                )
            )
        )
    }
}