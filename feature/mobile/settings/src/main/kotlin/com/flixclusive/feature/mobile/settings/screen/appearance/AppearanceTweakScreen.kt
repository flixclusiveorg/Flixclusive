package com.flixclusive.feature.mobile.settings.screen.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakScaffold
import com.flixclusive.feature.mobile.settings.TweakUI
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.collections.immutable.persistentListOf
import com.flixclusive.core.strings.R as LocaleR

@Destination<ExternalModuleGraph>
@Composable
internal fun AppearanceTweakScreen(
    navigator: NavigateBack,
    viewModel: AppearanceTweakViewModel = hiltViewModel(),
) {
    val uiPreferences by viewModel.preferences.collectAsStateWithLifecycle()

    TweakScaffold(
        title = stringResource(LocaleR.string.appearance),
        description = stringResource(LocaleR.string.appearance_settings_content_desc),
        navigateBack = navigator::navigateBack,
        tweaksProvider = {
            listOf(
                getGeneralTweaks(
                    uiPreferences = { uiPreferences },
                    onUpdatePreferences = viewModel::updateUserPrefs,
                )
            )
        },
    )
}

@Composable
private fun getGeneralTweaks(
    uiPreferences: () -> UiPreferences,
    onUpdatePreferences: (transform: suspend (t: UiPreferences) -> UiPreferences) -> Unit
): TweakGroup {
    val resources = LocalResources.current
    return TweakGroup(
        title = stringResource(LocaleR.string.general),
        tweaks =
            persistentListOf(
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.media_card_titles),
                    description = {
                        resources.getString(
                            LocaleR.string.media_card_titles_settings_description,
                        )
                    },
                    value = { uiPreferences().shouldShowTitleOnCards },
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(shouldShowTitleOnCards = it)
                        }
                    },
                ),
            ),
    )
}
