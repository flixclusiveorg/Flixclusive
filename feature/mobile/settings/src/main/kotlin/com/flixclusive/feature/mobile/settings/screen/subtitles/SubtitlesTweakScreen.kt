package com.flixclusive.feature.mobile.settings.screen.subtitles

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.player.CaptionEdgeTypePreference
import com.flixclusive.core.datastore.model.user.player.CaptionStylePreference
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.util.coroutines.FlxDispatchers.Companion.launchOnIO
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakScaffold
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.subtitles.component.ColorPicker
import com.flixclusive.feature.mobile.settings.screen.subtitles.component.ColorPickerWithAlpha
import com.flixclusive.feature.mobile.settings.screen.subtitles.component.SubtitlePreview
import com.flixclusive.feature.mobile.settings.screen.subtitles.component.availableColors
import com.flixclusive.feature.mobile.settings.util.uiText
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import java.util.Locale
import com.flixclusive.core.strings.R as LocaleR

private const val DEFAULT_TEXT_PREVIEW = "Abc"
private const val MAX_SUBTITLE_SIZE = 80F
private const val MIN_SUBTITLE_SIZE = 11F

@Destination<ExternalModuleGraph>
@Composable
internal fun SubtitlesTweakScreen(
    navigator: NavigateBack,
    viewModel: SubtitlesTweakViewModel = hiltViewModel()
) {
    val resources = LocalResources.current
    val subtitlePreferences by viewModel.preferences.collectAsStateWithLifecycle()

    val languages = remember {
        Locale
            .getAvailableLocales()
            .distinctBy { it.language }
            .associate {
                it.language to "${it.displayLanguage} [${it.language}]"
            }.toImmutableMap()
    }

    TweakScaffold(
        title = stringResource(LocaleR.string.subtitle),
        description = stringResource(LocaleR.string.subtitles_settings_content_desc),
        navigateBack = navigator::navigateBack,
        tweaksProvider = {
            listOf(
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.subtitle),
                    description = { resources.getString(LocaleR.string.subtitles_toggle_desc) },
                    value = { subtitlePreferences.isSubtitleEnabled },
                    onTweaked = {
                        viewModel.updateUserPrefs { prefs ->
                            prefs.copy(isSubtitleEnabled = it)
                        }
                    },
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.language),
                    value = { subtitlePreferences.subtitleLanguage },
                    description = {
                        val currentLanguage = subtitlePreferences.subtitleLanguage
                            .takeIf { it.isNotEmpty() }
                            ?: "en"

                        Locale
                            .Builder()
                            .setLanguage(currentLanguage)
                            .build()
                            .displayLanguage
                    },
                    enabledProvider = { subtitlePreferences.isSubtitleEnabled },
                    options = languages,
                    onTweaked = {
                        viewModel.updateUserPrefs { prefs ->
                            prefs.copy(subtitleLanguage = it)
                        }
                    },
                ),
                getUiTweaks(
                    subtitlePreferences = { subtitlePreferences },
                    onUpdatePreferences = viewModel::updateUserPrefs
                ),
            )
        }
    )
}

@Composable
private fun getUiTweaks(
    subtitlePreferences: () -> SubtitlesPreferences,
    onUpdatePreferences: (suspend (SubtitlesPreferences) -> SubtitlesPreferences) -> Unit,
): TweakGroup {
    val context = LocalContext.current

    val areSubtitlesAvailableProvider = { subtitlePreferences().isSubtitleEnabled }

    val fontStyle = remember { mutableStateOf(subtitlePreferences().subtitleFontStyle) }
    val edgeType = remember { mutableStateOf(subtitlePreferences().subtitleEdgeType) }
    val fontSize = remember { mutableFloatStateOf(subtitlePreferences().subtitleSize) }
    val fontStyles =
        remember {
            CaptionStylePreference.entries
                .associateWith { it.name }
                .toImmutableMap()
        }
    val edgeTypes =
        remember {
            CaptionEdgeTypePreference.entries
                .associateWith { it.name.replace("_", " ") }
                .toImmutableMap()
        }

    val alpha = remember { mutableFloatStateOf(Color(subtitlePreferences().subtitleBackgroundColor).alpha) }

    return TweakGroup(
        title = stringResource(LocaleR.string.style),
        enabledProvider = areSubtitlesAvailableProvider,
        tweaks = persistentListOf(
            TweakUI.CustomContentTweak(
                title = "Subtitle Preview",
                content = {
                    SubtitlePreview(
                        subtitlePreferencesProvider = subtitlePreferences,
                        areSubtitlesAvailableProvider = areSubtitlesAvailableProvider,
                    )
                },
            ),
            TweakUI.SliderTweak(
                title = stringResource(LocaleR.string.subtitles_size),
                description = {
                    "${
                        String.format(
                            Locale.getDefault(),
                            "%.2f",
                            fontSize.floatValue,
                        )
                    } sp"
                },
                value = { subtitlePreferences().subtitleSize },
                range = MIN_SUBTITLE_SIZE..MAX_SUBTITLE_SIZE,
                enabledProvider = areSubtitlesAvailableProvider,
                onTweaked = {
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(subtitleSize = it)
                    }
                },
            ),
            TweakUI.ListTweak(
                title = stringResource(LocaleR.string.subtitles_font_style),
                description = { fontStyle.value.toString() },
                value = { subtitlePreferences().subtitleFontStyle },
                options = fontStyles,
                enabledProvider = areSubtitlesAvailableProvider,
                endContent = {
                    Text(
                        text = DEFAULT_TEXT_PREVIEW,
                        style =
                            MaterialTheme.typography.labelLarge.run {
                                when (fontStyle.value) {
                                    CaptionStylePreference.Normal -> {
                                        copy(
                                            fontWeight = FontWeight.Normal,
                                        )
                                    }

                                    CaptionStylePreference.Bold -> {
                                        copy(
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }

                                    CaptionStylePreference.Italic -> {
                                        copy(
                                            fontStyle = FontStyle.Italic,
                                        )
                                    }

                                    CaptionStylePreference.Monospace -> {
                                        copy(
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                }
                            },
                    )
                },
                onTweaked = {
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(subtitleFontStyle = it)
                    }
                },
            ),
            TweakUI.ListTweak(
                title = stringResource(LocaleR.string.subtitles_edge_type),
                description = { edgeType.value.uiText.asString(context) },
                value = { subtitlePreferences().subtitleEdgeType },
                options = edgeTypes,
                enabledProvider = areSubtitlesAvailableProvider,
                onTweaked = {
                    onUpdatePreferences { oldValue ->
                        oldValue.copy(subtitleEdgeType = it)
                    }
                },
            ),
            TweakUI.CustomContentTweak(
                title = stringResource(LocaleR.string.subtitles_color),
                content = {
                    ColorPicker(
                        title = stringResource(LocaleR.string.subtitles_color),
                        description = stringResource(LocaleR.string.subtitles_color_desc),
                        selectedColor = remember { subtitlePreferences().subtitleColor },
                        colors = availableColors,
                        enabledProvider = areSubtitlesAvailableProvider,
                        onPick = {
                            launchOnIO {
                                onUpdatePreferences { oldValue ->
                                    oldValue.copy(subtitleColor = it.toArgb())
                                }
                            }
                        },
                    )
                },
            ),
            TweakUI.CustomContentTweak(
                title = stringResource(LocaleR.string.subtitles_background_color),
                content = {
                    ColorPickerWithAlpha(
                        title = stringResource(LocaleR.string.subtitles_background_color),
                        description = stringResource(LocaleR.string.subtitles_background_color_desc),
                        selectedColor = remember { subtitlePreferences().subtitleBackgroundColor },
                        colors = availableColors,
                        enabledProvider = areSubtitlesAvailableProvider,
                        transparencyProvider = { alpha.floatValue },
                        onAlphaChange = { alpha.floatValue = it },
                        onPick = { newColor ->
                            launchOnIO {
                                onUpdatePreferences { oldValue ->
                                    val newColorWithAlpha = newColor.copy(alpha.floatValue)
                                    oldValue.copy(subtitleBackgroundColor = newColorWithAlpha.toArgb())
                                }
                            }
                        },
                    )
                },
            ),
        ),
    )
}
