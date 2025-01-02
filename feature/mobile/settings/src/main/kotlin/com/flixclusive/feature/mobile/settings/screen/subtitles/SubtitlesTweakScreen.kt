package com.flixclusive.feature.mobile.settings.screen.subtitles

import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalAppSettings
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalScaffoldNavigator
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.getCurrentSettingsViewModel
import com.flixclusive.model.datastore.player.CaptionEdgeTypePreference
import com.flixclusive.model.datastore.player.CaptionStylePreference
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import java.util.Locale
import com.flixclusive.core.locale.R as LocaleR

private const val DEFAULT_TEXT_PREVIEW = "Abc"
private const val MAX_SUBTITLE_SIZE = 80F
private const val MIN_SUBTITLE_SIZE = 11F

internal object SubtitlesTweakScreen : BaseTweakScreen {
    @Composable
    override fun getTitle(): String
        = stringResource(LocaleR.string.subtitle)

    @Composable
    override fun getDescription(): String
        = stringResource(LocaleR.string.subtitles_settings_content_desc)

    @Composable
    override fun getTweaks(): List<Tweak> {
        val appSettings = LocalAppSettings.current
        val viewModel = getCurrentSettingsViewModel()
        val onTweaked = viewModel::onChangeAppSettings

        val areSubtitlesAvailable = remember { mutableStateOf(appSettings.isSubtitleEnabled) }

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
                    onTweaked(appSettings.copy(isSubtitleEnabled = it))
                    true
                },
            ),
            getUiTweaks(),
            TweakUI.ListTweak(
                title = stringResource(LocaleR.string.language),
                description = remember { Locale(appSettings.subtitleLanguage).displayLanguage },
                value = remember { mutableStateOf(appSettings.subtitleLanguage) },
                enabled = appSettings.isSubtitleEnabled,
                options = languages,
                onTweaked = {
                    onTweaked(appSettings.copy(subtitleLanguage = it))
                    true
                }
            ),
        )
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalScaffoldNavigator.current
        BackHandler {
            navigator?.navigateBack()
        }

        super.Content()
    }


    @Composable
    private fun getUiTweaks(): TweakGroup {
        val appSettings = LocalAppSettings.current
        val viewModel = getCurrentSettingsViewModel()
        val onTweaked = viewModel::onChangeAppSettings

        val fontStyle = remember { mutableStateOf(appSettings.subtitleFontStyle) }
        val edgeType = remember { mutableStateOf(appSettings.subtitleEdgeType) }
        val fontSize = remember { mutableFloatStateOf(appSettings.subtitleSize) }
        val fontColor = remember { mutableStateOf(Color(appSettings.subtitleColor)) }
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
            enabled = appSettings.isSubtitleEnabled,
            tweaks = persistentListOf(
                TweakUI.SliderTweak(
                    title = stringResource(LocaleR.string.subtitles_size),
                    description = "${String.format(Locale.getDefault(), "%.2f", fontSize.floatValue)} sp",
                    value = fontSize,
                    range = MIN_SUBTITLE_SIZE..MAX_SUBTITLE_SIZE,
                    enabled = appSettings.isSubtitleEnabled,
                    onTweaked = {
                        onTweaked(appSettings.copy(subtitleSize = it))
                        true
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.subtitles_font_style),
                    description = fontStyle.value.toString(),
                    value = fontStyle,
                    options = fontStyles,
                    enabled = appSettings.isSubtitleEnabled,
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
                        onTweaked(appSettings.copy(subtitleFontStyle = it))
                        true
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.subtitles_edge_type),
                    description = appSettings.subtitleEdgeType.toUiText().asString(),
                    value = edgeType,
                    options = edgeTypes,
                    enabled = appSettings.isSubtitleEnabled,
                    onTweaked = {
                        onTweaked(appSettings.copy(subtitleEdgeType = it))
                        true
                    }
                ),
                TweakUI.CustomContentTweak(
                    title = stringResource(LocaleR.string.subtitles_color),
                    description = stringResource(LocaleR.string.subtitles_color_desc),
                    value = fontColor,
                    enabled = appSettings.isSubtitleEnabled,
                    onTweaked = {
                        onTweaked(appSettings.copy(subtitleColor = it.toArgb()))
                        true
                    },
                    content = {
                        // TODO("Create custom color picker for font color")
                    }
                ),
                TweakUI.CustomContentTweak(
                    value = remember { mutableStateOf(Color(appSettings.subtitleBackgroundColor)) },
                    title = stringResource(LocaleR.string.subtitles_background_color),
                    description = stringResource(LocaleR.string.subtitles_background_color_desc),
                    enabled = appSettings.isSubtitleEnabled,
                    content = {
                        // TODO("Create custom color picker for background color")
                    }
                ),
            )
        )
    }
}