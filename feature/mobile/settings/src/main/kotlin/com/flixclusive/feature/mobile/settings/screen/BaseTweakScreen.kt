package com.flixclusive.feature.mobile.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.painter.Painter
import androidx.datastore.preferences.core.Preferences
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakScaffold
import com.flixclusive.model.datastore.FlixclusivePrefs
import kotlinx.coroutines.flow.StateFlow

internal interface BaseTweakScreen<T : FlixclusivePrefs> {
    val key: Preferences.Key<String>
    val preferencesAsState: StateFlow<T>
    val onUpdatePreferences: suspend (suspend (T) -> T) -> Boolean

    val isSubNavigation: Boolean get() = false

    @Composable
    @ReadOnlyComposable
    fun getTitle(): String

    @Composable
    @ReadOnlyComposable
    fun getDescription(): String

    @Composable
    @ReadOnlyComposable
    fun getIconPainter(): Painter? = null

    @Composable
    fun getTweaks(): List<Tweak>

    @Composable
    fun Content() {
        TweakScaffold(
            title = getTitle(),
            description = getDescription(),
            tweaksProvider = { getTweaks() }
        )
    }
}