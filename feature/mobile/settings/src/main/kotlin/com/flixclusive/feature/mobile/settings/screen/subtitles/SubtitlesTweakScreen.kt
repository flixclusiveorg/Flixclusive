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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import com.flixclusive.feature.mobile.settings.util.LocalScaffoldNavigator
import com.flixclusive.model.datastore.user.SubtitlesPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.datastore.user.player.CaptionEdgeTypePreference
import com.flixclusive.model.datastore.user.player.CaptionStylePreference
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import com.flixclusive.core.locale.R as LocaleR

private const val DEFAULT_TEXT_PREVIEW = "Abc"
private const val MAX_SUBTITLE_SIZE = 80F
private const val MIN_SUBTITLE_SIZE = 11F

internal class SubtitlesTweakScreen(
    viewModel: SettingsViewModel
) : BaseTweakScreen<SubtitlesPreferences> {
    override val key = UserPreferences.SUBTITLES_PREFS_KEY
    override val preferencesAsState: StateFlow<SubtitlesPreferences>
        = viewModel.getUserPrefsAsState<SubtitlesPreferences>(key)
    override val onUpdatePreferences: suspend (suspend (SubtitlesPreferences) -> SubtitlesPreferences) -> Boolean
        = { viewModel.updateUserPrefs(key, it) }

    override val isSubNavigation: Boolean = true

    @Composable
    override fun getTitle(): String
        = stringResource(LocaleR.string.subtitle)

    @Composable
    override fun getDescription(): String
        = stringResource(LocaleR.string.subtitles_settings_content_desc)

    @Composable
    override fun getTweaks(): List<Tweak> {
        val subtitlePreferences by preferencesAsState.collectAsStateWithLifecycle()

        val areSubtitlesAvailable = remember { mutableStateOf(subtitlePreferences.isSubtitleEnabled) }
        val currentSubtitleLanguage = remember { mutableStateOf(subtitlePreferences.subtitleLanguage) }

        val languages = remember {
            Locale.getAvailableLocales()
                .distinctBy { it.language }
                .associate {
                    it.language to "${it.displayLanguage} [${it.language}]"
                }
                .toImmutableMap()
        }

        return listOf(
            TweakUI.SwitchTweak(
                title = stringResource(LocaleR.string.subtitle),
                description = stringResource(LocaleR.string.subtitles_toggle_desc),
                value = areSubtitlesAvailable,
                onTweaked = {
                    onUpdatePreferences { prefs ->
                        prefs.copy(isSubtitleEnabled = it)
                    }
                },
            ),
            getUiTweaks(
                subtitlePreferences = subtitlePreferences
            ),
            TweakUI.ListTweak(
                title = stringResource(LocaleR.string.language),
                value = currentSubtitleLanguage,
                description = Locale(currentSubtitleLanguage.value).displayLanguage,
                enabled = subtitlePreferences.isSubtitleEnabled,
                options = languages,
                onTweaked = {
                    onUpdatePreferences { prefs ->
                        prefs.copy(subtitleLanguage = it)
                    }
                }
            ),
        )
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalScaffoldNavigator.current
        BackHandler {
            navigator?.navigateBack(
                backNavigationBehavior = BackNavigationBehavior.PopLatest
            )
        }

        super.Content()
    }


    @Composable
    private fun getUiTweaks(
        subtitlePreferences: SubtitlesPreferences,
    ): TweakGroup {
        val fontStyle = remember { mutableStateOf(subtitlePreferences.subtitleFontStyle) }
        val edgeType = remember { mutableStateOf(subtitlePreferences.subtitleEdgeType) }
        val fontSize = remember { mutableFloatStateOf(subtitlePreferences.subtitleSize) }
        val fontColor = remember { mutableStateOf(Color(subtitlePreferences.subtitleColor)) }
        val fontStyles = remember {
            CaptionStylePreference.entries
                .associateWith { it.name }
                .toImmutableMap()
        }
        val edgeTypes = remember {
            CaptionEdgeTypePreference.entries
                .associateWith { it.name.replace("_", " ") }
                .toImmutableMap()
        }

        return TweakGroup(
            title = stringResource(LocaleR.string.style),
            enabled = subtitlePreferences.isSubtitleEnabled,
            tweaks = persistentListOf(
                TweakUI.SliderTweak(
                    title = stringResource(LocaleR.string.subtitles_size),
                    description = "${String.format(Locale.getDefault(), "%.2f", fontSize.floatValue)} sp",
                    value = fontSize,
                    range = MIN_SUBTITLE_SIZE..MAX_SUBTITLE_SIZE,
                    enabled = subtitlePreferences.isSubtitleEnabled,
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(subtitleSize = it)
                        }
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.subtitles_font_style),
                    description = fontStyle.value.toString(),
                    value = fontStyle,
                    options = fontStyles,
                    enabled = subtitlePreferences.isSubtitleEnabled,
                    endContent = {
                        Text(
                            text = DEFAULT_TEXT_PREVIEW,
                            style = MaterialTheme.typography.labelLarge.run {
                                when (fontStyle.value) {
                                    CaptionStylePreference.Normal -> copy(
                                        fontWeight = FontWeight.Normal
                                    )
                                    CaptionStylePreference.Bold -> copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                    CaptionStylePreference.Italic -> copy(
                                        fontStyle = FontStyle.Italic
                                    )
                                    CaptionStylePreference.Monospace -> copy(
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        )
                    },
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(subtitleFontStyle = it)
                        }
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.subtitles_edge_type),
                    description = subtitlePreferences.subtitleEdgeType.toUiText().asString(),
                    value = edgeType,
                    options = edgeTypes,
                    enabled = subtitlePreferences.isSubtitleEnabled,
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(subtitleEdgeType = it)
                        }
                    }
                ),
                TweakUI.CustomContentTweak(
                    title = stringResource(LocaleR.string.subtitles_color),
                    description = stringResource(LocaleR.string.subtitles_color_desc),
                    value = fontColor,
                    enabled = subtitlePreferences.isSubtitleEnabled,
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(subtitleColor = it.toArgb())
                        }
                    },
                    content = {
                        // TODO("Create custom color picker for font color")
                    }
                ),
                TweakUI.CustomContentTweak(
                    value = remember { mutableStateOf(Color(subtitlePreferences.subtitleBackgroundColor)) },
                    title = stringResource(LocaleR.string.subtitles_background_color),
                    description = stringResource(LocaleR.string.subtitles_background_color_desc),
                    enabled = subtitlePreferences.isSubtitleEnabled,
                    content = {
                        // TODO("Create custom color picker for background color")
                    }
                ),
            )
        )
    }
}