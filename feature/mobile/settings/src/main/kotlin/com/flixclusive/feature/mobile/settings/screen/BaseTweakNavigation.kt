package com.flixclusive.feature.mobile.settings.screen

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.screen.root.SettingsScreenNavigator
import com.flixclusive.model.datastore.FlixclusivePrefs
import kotlinx.coroutines.flow.StateFlow

internal interface BaseTweakNavigation : BaseTweakScreen<FlixclusivePrefs> {
    override val key: Preferences.Key<String>
        get() = throw NotImplementedError()
    override val preferencesAsState: StateFlow<FlixclusivePrefs>
        get() = throw NotImplementedError()
    override val onUpdatePreferences: suspend (suspend (FlixclusivePrefs) -> FlixclusivePrefs) -> Boolean
        get() = throw NotImplementedError()

    @Composable
    override fun getTitle(): String {
        TODO("Not yet implemented")
    }

    fun onClick(navigator: SettingsScreenNavigator)

    @Composable
    override fun getTweaks(): List<Tweak> = listOf()

    @Composable
    override fun Content() = Unit

    @Composable
    override fun getDescription() = ""
}
