package com.flixclusive.feature.mobile.settings.screen.subtitles

import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.datastore.model.user.player.CaptionEdgeTypePreference
import com.flixclusive.core.datastore.model.user.player.CaptionStylePreference
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.launchOnIO
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import com.flixclusive.feature.mobile.settings.screen.subtitles.component.ColorPicker
import com.flixclusive.feature.mobile.settings.screen.subtitles.component.ColorPickerWithAlpha
import com.flixclusive.feature.mobile.settings.screen.subtitles.component.SubtitlePreview
import com.flixclusive.feature.mobile.settings.screen.subtitles.component.availableColors
import com.flixclusive.feature.mobile.settings.util.LocalScaffoldNavigator
import com.flixclusive.feature.mobile.settings.util.uiText
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import com.flixclusive.core.strings.R as LocaleR

private const val DEFAULT_TEXT_PREVIEW = "Abc"
private const val MAX_SUBTITLE_SIZE = 80F
private const val MIN_SUBTITLE_SIZE = 11F

internal class SubtitlesTweakScreen(
    private val viewModel: SettingsViewModel,
) : BaseTweakScreen<SubtitlesPreferences> {
    override val key = UserPreferences.SUBTITLES_PREFS_KEY
    override val preferencesAsState: StateFlow<SubtitlesPreferences> =
        viewModel.getUserPrefsAsState<SubtitlesPreferences>(key)

    override suspend fun onUpdatePreferences(
        transform: suspend (t: SubtitlesPreferences) -> SubtitlesPreferences,
    ): Boolean {
        return viewModel.updateUserPrefs(key, transform)
    }

    override val isSubNavigation: Boolean = true

    @Composable
    override fun getTitle(): String = stringResource(LocaleR.string.subtitle)

    @Composable
    override fun getDescription(): String = stringResource(LocaleR.string.subtitles_settings_content_desc)

    @Composable
    override fun getTweaks(): List<Tweak> {
        val context = LocalContext.current
        val subtitlePreferences by preferencesAsState.collectAsStateWithLifecycle()

        val areSubtitlesAvailable = remember { mutableStateOf(subtitlePreferences.isSubtitleEnabled) }
        val currentSubtitleLanguage = remember { mutableStateOf(subtitlePreferences.subtitleLanguage) }

        val languages =
            remember {
                Locale
                    .getAvailableLocales()
                    .distinctBy { it.language }
                    .associate {
                        it.language to "${it.displayLanguage} [${it.language}]"
                    }.toImmutableMap()
            }

        return listOf(
            TweakUI.SwitchTweak(
                title = stringResource(LocaleR.string.subtitle),
                descriptionProvider = { context.getString(LocaleR.string.subtitles_toggle_desc) },
                value = areSubtitlesAvailable,
                onTweaked = {
                    onUpdatePreferences { prefs ->
                        prefs.copy(isSubtitleEnabled = it)
                    }
                },
            ),
            TweakUI.ListTweak(
                title = stringResource(LocaleR.string.language),
                value = currentSubtitleLanguage,
                descriptionProvider = {
                    Locale
                        .Builder()
                        .setLanguage(currentSubtitleLanguage.value)
                        .build()
                        .displayLanguage
                },
                enabledProvider = { areSubtitlesAvailable.value },
                options = languages,
                onTweaked = {
                    onUpdatePreferences { prefs ->
                        prefs.copy(subtitleLanguage = it)
                    }
                },
            ),
            getUiTweaks(
                areSubtitlesAvailableProvider = { areSubtitlesAvailable.value },
                subtitlePreferencesProvider = { subtitlePreferences },
            ),
        )
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalScaffoldNavigator.current
        BackHandler {
            navigator?.navigateBack(
                backNavigationBehavior = BackNavigationBehavior.PopLatest,
            )
        }

        super.Content()
    }

    @Composable
    private fun getUiTweaks(
        areSubtitlesAvailableProvider: () -> Boolean,
        subtitlePreferencesProvider: () -> SubtitlesPreferences,
    ): TweakGroup {
        val context = LocalContext.current

        val fontStyle = remember { mutableStateOf(subtitlePreferencesProvider().subtitleFontStyle) }
        val edgeType = remember { mutableStateOf(subtitlePreferencesProvider().subtitleEdgeType) }
        val fontSize = remember { mutableFloatStateOf(subtitlePreferencesProvider().subtitleSize) }
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

        val alpha = remember { mutableFloatStateOf(Color(subtitlePreferencesProvider().subtitleBackgroundColor).alpha) }

        return TweakGroup(
            title = stringResource(LocaleR.string.style),
            enabledProvider = areSubtitlesAvailableProvider,
            tweaks =
                persistentListOf(
                    TweakUI.CustomContentTweak(
                        title = "Subtitle Preview",
                        content = {
                            SubtitlePreview(
                                subtitlePreferencesProvider = subtitlePreferencesProvider,
                                areSubtitlesAvailableProvider = areSubtitlesAvailableProvider,
                            )
                        },
                    ),
                    TweakUI.SliderTweak(
                        title = stringResource(LocaleR.string.subtitles_size),
                        descriptionProvider = {
                            "${
                                String.format(
                                    Locale.getDefault(),
                                    "%.2f",
                                    fontSize.floatValue,
                                )
                            } sp"
                        },
                        value = fontSize,
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
                        descriptionProvider = { fontStyle.value.toString() },
                        value = fontStyle,
                        options = fontStyles,
                        enabledProvider = areSubtitlesAvailableProvider,
                        endContent = {
                            Text(
                                text = DEFAULT_TEXT_PREVIEW,
                                style =
                                    MaterialTheme.typography.labelLarge.run {
                                        when (fontStyle.value) {
                                            CaptionStylePreference.Normal ->
                                                copy(
                                                    fontWeight = FontWeight.Normal,
                                                )

                                            CaptionStylePreference.Bold ->
                                                copy(
                                                    fontWeight = FontWeight.Bold,
                                                )

                                            CaptionStylePreference.Italic ->
                                                copy(
                                                    fontStyle = FontStyle.Italic,
                                                )

                                            CaptionStylePreference.Monospace ->
                                                copy(
                                                    fontFamily = FontFamily.Monospace,
                                                )
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
                        descriptionProvider = { edgeType.value.uiText.asString(context) },
                        value = edgeType,
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
                                selectedColor = remember { subtitlePreferencesProvider().subtitleColor },
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
                                selectedColor = remember { subtitlePreferencesProvider().subtitleBackgroundColor },
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
}
