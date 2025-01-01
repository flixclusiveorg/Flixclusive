package com.flixclusive.feature.mobile.settings.screen.player

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.player.PlayerAdvancedValues.getAvailableBufferSizes
import com.flixclusive.feature.mobile.settings.screen.player.PlayerAdvancedValues.getAvailableCacheSizes
import com.flixclusive.feature.mobile.settings.screen.player.PlayerAdvancedValues.playerBufferLengths
import com.flixclusive.feature.mobile.settings.screen.subtitles.SubtitlesTweakScreen
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalAppSettings
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.LocalScaffoldNavigator
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.getCurrentSettingsViewModel
import com.flixclusive.model.datastore.player.DecoderPriority
import com.flixclusive.model.datastore.player.PlayerQuality
import com.flixclusive.model.datastore.player.ResizeMode
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import java.util.Locale
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal object PlayerTweakScreen : BaseTweakScreen {
    @Composable
    @ReadOnlyComposable
    override fun getTitle()
        = stringResource(LocaleR.string.player)

    @Composable
    @ReadOnlyComposable
    override fun getDescription()
        = stringResource(LocaleR.string.player_settings_content_desc)

    @Composable
    override fun getIconPainter()
        = painterResource(UiCommonR.drawable.play_outline_circle)

    /* TODO: Optimize Player settings screen */
    @Composable
    override fun getTweaks(): List<Tweak> {
        return listOf(
            getGeneralTweaks(),
            getAudioTweaks(),
            getAdvancedTweaks()
        )
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Composable
    private fun getGeneralTweaks(): TweakGroup {
        val context = LocalContext.current
        val navigator = LocalScaffoldNavigator.current!!

        val appSettings = LocalAppSettings.current
        val viewModel = getCurrentSettingsViewModel()
        val onTweaked = viewModel::onChangeAppSettings

        val formatInSeconds = fun (amount: Long): String {
            return context.getString(LocaleR.string.n_seconds_format, amount)
        }

        val selectedSeekAmount = remember(appSettings.preferredSeekAmount) {
            val amountInSeconds = appSettings.preferredSeekAmount / 1000
            formatInSeconds(amountInSeconds)
        }

        val selectedResizeMode = remember(appSettings.preferredResizeMode) {
            val mode = ResizeMode.entries
                .find { it.ordinal == appSettings.preferredResizeMode }
                ?: ResizeMode.Fit

            mode.toUiText().asString(context)
        }

        val availableQualities = remember {
            PlayerQuality.entries
                .associateWith { it.qualityName.asString(context) }
                .toPersistentMap()
        }

        return TweakGroup(
            title = stringResource(LocaleR.string.video),
            tweaks = persistentListOf(
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.resize_mode),
                    description = selectedResizeMode,
                    value = rememberSaveable { mutableIntStateOf(appSettings.preferredResizeMode) },
                    onTweaked = {
                        onTweaked(appSettings.copy(preferredResizeMode = it))
                        true
                    },
                    options = persistentMapOf(
                        ResizeMode.Fit.mode to ResizeMode.Fit.toUiText().asString(context),
                        ResizeMode.Fill.mode to ResizeMode.Fill.toUiText().asString(context),
                        ResizeMode.Zoom.mode to ResizeMode.Zoom.toUiText().asString(context)
                    )
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.preferred_quality),
                    description = appSettings.preferredQuality.qualityName.asString(),
                    value = remember { mutableStateOf(appSettings.preferredQuality) },
                    options = availableQualities,
                    onTweaked = {
                        onTweaked(appSettings.copy(preferredQuality = it))
                        true
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.seek_length_label),
                    description = selectedSeekAmount,
                    value = remember { mutableLongStateOf(appSettings.preferredSeekAmount) },
                    options = persistentMapOf(
                        5000L to formatInSeconds(5),
                        10000L to formatInSeconds(10),
                        30000L to formatInSeconds(30)
                    ),
                    onTweaked = {
                        onTweaked(appSettings.copy(preferredSeekAmount = it))
                        true
                    }
                ),
                TweakUI.ClickableTweak(
                    title = stringResource(LocaleR.string.subtitle),
                    description = stringResource(LocaleR.string.subtitles_settings_content_desc),
                    onClick = {
                        navigator.navigateTo(
                            pane = ListDetailPaneScaffoldRole.Detail,
                            content = SubtitlesTweakScreen
                        )
                    }
                )
            )
        )
    }

    @Composable
    private fun getUiTweaks(): TweakGroup {
        val appSettings = LocalAppSettings.current
        val viewModel = getCurrentSettingsViewModel()
        val onTweaked = viewModel::onChangeAppSettings

        return TweakGroup(
            title = stringResource(LocaleR.string.user_interface),
            tweaks = persistentListOf(
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.reverse_player_time),
                    description = stringResource(LocaleR.string.reverse_player_time_desc),
                    value = remember { mutableStateOf(appSettings.isPlayerTimeReversed) },
                    onTweaked = {
                        onTweaked(appSettings.copy(isPlayerTimeReversed = it))
                        true
                    }
                ),
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.pip_mode),
                    description = stringResource(LocaleR.string.pip_mode_desc),
                    value = remember { mutableStateOf(appSettings.isPiPModeEnabled) },
                    onTweaked = {
                        onTweaked(appSettings.copy(isPiPModeEnabled = it))
                        true
                    }
                ),

            )
        )
    }

    @Composable
    private fun getAudioTweaks(): TweakGroup {
        val appSettings = LocalAppSettings.current
        val viewModel = getCurrentSettingsViewModel()
        val onTweaked = viewModel::onChangeAppSettings

        val languages = remember {
            Locale.getAvailableLocales()
                .distinctBy { it.language }
                .associate {
                    it.language to "${it.displayLanguage} [${it.language}]"
                }
                .toImmutableMap()
        }

        val useVolumeBooster = remember { mutableStateOf(appSettings.isUsingVolumeBoost) }

        return TweakGroup(
            title = stringResource(LocaleR.string.audio),
            tweaks = persistentListOf(
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.volume_booster),
                    description = stringResource(LocaleR.string.volume_booster_desc),
                    value = useVolumeBooster,
                    onTweaked = {
                        onTweaked(appSettings.copy(isUsingVolumeBoost = it))
                        true
                    },
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.preferred_audio_language),
                    description = Locale(appSettings.preferredAudioLanguage).displayLanguage,
                    value = remember { mutableStateOf(appSettings.preferredAudioLanguage) },
                    options = languages,
                    onTweaked = {
                        onTweaked(appSettings.copy(preferredAudioLanguage = it))
                        true
                    }
                )
            )
        )
    }

    @Composable
    private fun getAdvancedTweaks(): TweakGroup {
        val context = LocalContext.current

        val appSettings = LocalAppSettings.current
        val viewModel = getCurrentSettingsViewModel()
        val onTweaked = viewModel::onChangeAppSettings

        val availableDecoders = remember {
            DecoderPriority.entries
                .associateWith { it.toUiText().asString(context) }
                .toPersistentMap()
        }

        return TweakGroup(
            title = stringResource(LocaleR.string.advanced),
            tweaks = persistentListOf(
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.release_player),
                    description = stringResource(LocaleR.string.release_player_desc),
                    value = remember { mutableStateOf(appSettings.shouldReleasePlayer) },
                    onTweaked = {
                        onTweaked(appSettings.copy(shouldReleasePlayer = it))
                        true
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.decoder_priority),
                    description = stringResource(LocaleR.string.decoder_priority_description),
                    value = remember { mutableStateOf(appSettings.decoderPriority) },
                    options = availableDecoders,
                    onTweaked = {
                        onTweaked(appSettings.copy(decoderPriority = it))
                        true
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.video_cache_size),
                    description = stringResource(LocaleR.string.video_cache_size_label),
                    value = remember { mutableLongStateOf(appSettings.preferredDiskCacheSize) },
                    options = getAvailableCacheSizes(context),
                    onTweaked = {
                        onTweaked(appSettings.copy(preferredDiskCacheSize = it))
                        true
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.video_buffer_size),
                    description = stringResource(LocaleR.string.video_buffer_size_label),
                    value = remember { mutableLongStateOf(appSettings.preferredBufferCacheSize) },
                    options = getAvailableBufferSizes(context),
                    onTweaked = {
                        onTweaked(appSettings.copy(preferredBufferCacheSize = it))
                        true
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.video_buffer_max_length),
                    description = stringResource(LocaleR.string.video_buffer_max_length_desc),
                    value = remember { mutableLongStateOf(appSettings.preferredVideoBufferMs) },
                    options = playerBufferLengths,
                    onTweaked = {
                        onTweaked(appSettings.copy(preferredVideoBufferMs = it))
                        true
                    }
                )
            )
        )
    }

}