package com.flixclusive.feature.mobile.settings.screen.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

internal class AppearanceTweakScreen(
    private val viewModel: SettingsViewModel,
) : BaseTweakScreen<UiPreferences> {
    override val key = UserPreferences.UI_PREFS_KEY
    override val preferencesAsState: StateFlow<UiPreferences> =
        viewModel.getUserPrefsAsState<UiPreferences>(key)

    override suspend fun onUpdatePreferences(transform: suspend (t: UiPreferences) -> UiPreferences): Boolean {
        return viewModel.updateUserPrefs(key, transform)
    }

    @Composable
    override fun getTitle(): String = stringResource(LocaleR.string.appearance)

    @Composable
    override fun getIconPainter(): Painter = painterResource(UiCommonR.drawable.appearance_settings)

    @Composable
    override fun getDescription(): String = stringResource(LocaleR.string.appearance_settings_content_desc)

    @Composable
    override fun getTweaks(): List<Tweak> {
        val uiPreferences by preferencesAsState.collectAsStateWithLifecycle()

        return listOf(
            getGeneralTweaks(shouldShowTitleOnCardsProvider = { uiPreferences.shouldShowTitleOnCards }),
        )
    }

    @Composable
    private fun getGeneralTweaks(shouldShowTitleOnCardsProvider: () -> Boolean): TweakGroup {
        val context = LocalContext.current
        return TweakGroup(
            title = stringResource(LocaleR.string.general),
            tweaks =
                persistentListOf(
                    TweakUI.SwitchTweak(
                        title = stringResource(LocaleR.string.film_card_titles),
                        descriptionProvider = {
                            context.getString(
                                LocaleR.string.film_card_titles_settings_description,
                            )
                        },
                        value = remember { mutableStateOf(shouldShowTitleOnCardsProvider()) },
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(shouldShowTitleOnCards = it)
                            }
                        },
                    ),
                ),
        )
    }
}
