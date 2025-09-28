package com.flixclusive.feature.mobile.settings.screen

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.core.datastore.model.FlixclusivePrefs
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.screen.root.SettingsScreenNavigator
import kotlinx.coroutines.flow.StateFlow

internal interface BaseTweakNavigation : BaseTweakScreen<FlixclusivePrefs> {
    override val key: Preferences.Key<String>
        get() = throw NotImplementedError()
    override val preferencesAsState: StateFlow<FlixclusivePrefs>
        get() = throw NotImplementedError()

    override suspend fun onUpdatePreferences(transform: suspend (t: FlixclusivePrefs) -> FlixclusivePrefs): Boolean {
        throw NotImplementedError()
    }

    fun onClick(navigator: SettingsScreenNavigator)

    @Composable
    override fun getTweaks(): List<Tweak> = listOf()

    @Composable
    override fun Content() = Unit

    @Composable
    override fun getDescription() = ""
}
