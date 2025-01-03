package com.flixclusive.feature.mobile.settings.screen.player

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.player.PlayerAdvancedValues.getAvailableBufferSizes
import com.flixclusive.feature.mobile.settings.screen.player.PlayerAdvancedValues.getAvailableCacheSizes
import com.flixclusive.feature.mobile.settings.screen.player.PlayerAdvancedValues.playerBufferLengths
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import com.flixclusive.feature.mobile.settings.util.LocalScaffoldNavigator
import com.flixclusive.model.datastore.user.PlayerPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.datastore.user.player.DecoderPriority
import com.flixclusive.model.datastore.user.player.PlayerQuality
import com.flixclusive.model.datastore.user.player.ResizeMode
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

internal class PlayerTweakScreen(
    private val viewModel: SettingsViewModel
) : BaseTweakScreen<PlayerPreferences> {
    override val key = UserPreferences.PLAYER_PREFS_KEY
    override val preferencesAsState: StateFlow<PlayerPreferences>
        = viewModel.getUserPrefsAsState<PlayerPreferences>(key)
    override val onUpdatePreferences: suspend (suspend (PlayerPreferences) -> PlayerPreferences) -> Boolean
        = { viewModel.updateUserPrefs(key, it) }

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

    @Composable
    override fun getTweaks(): List<Tweak> {
        val playerPreferences by preferencesAsState.collectAsStateWithLifecycle()
        
        return listOf(
            getGeneralTweaks(playerPreferences),
            getAudioTweaks(playerPreferences),
            getAdvancedTweaks(playerPreferences)
        )
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Composable
    private fun getGeneralTweaks(
        playerPreferences: PlayerPreferences
    ): TweakGroup {
        val context = LocalContext.current
        val navigator = LocalScaffoldNavigator.current!!

        val formatInSeconds = fun (amount: Long): String {
            return context.getString(LocaleR.string.n_seconds_format, amount)
        }

        val selectedSeekAmount = remember(playerPreferences.seekAmount) {
            val amountInSeconds = playerPreferences.seekAmount / 1000
            formatInSeconds(amountInSeconds)
        }

        val selectedResizeMode = remember(playerPreferences.resizeMode) {
            val mode = ResizeMode.entries
                .find { it.ordinal == playerPreferences.resizeMode }
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
                    value = remember { mutableIntStateOf(playerPreferences.resizeMode) },
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(resizeMode = it)
                        }
                    },
                    options = persistentMapOf(
                        ResizeMode.Fit.mode to ResizeMode.Fit.toUiText().asString(context),
                        ResizeMode.Fill.mode to ResizeMode.Fill.toUiText().asString(context),
                        ResizeMode.Zoom.mode to ResizeMode.Zoom.toUiText().asString(context)
                    )
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.preferred_quality),
                    description = playerPreferences.quality.qualityName.asString(),
                    value = remember { mutableStateOf(playerPreferences.quality) },
                    options = availableQualities,
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(quality = it)
                        }
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.seek_length_label),
                    description = selectedSeekAmount,
                    value = remember { mutableLongStateOf(playerPreferences.seekAmount) },
                    options = persistentMapOf(
                        5000L to formatInSeconds(5),
                        10000L to formatInSeconds(10),
                        30000L to formatInSeconds(30)
                    ),
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(seekAmount = it)
                        }
                    }
                ),
                TweakUI.ClickableTweak(
                    title = stringResource(LocaleR.string.subtitle),
                    description = stringResource(LocaleR.string.subtitles_settings_content_desc),
                    onClick = {
                        navigator.navigateTo(
                            pane = ListDetailPaneScaffoldRole.Detail,
                            content = UserPreferences.SUBTITLES_PREFS_KEY.name
                        )
                    }
                )
            )
        )
    }

    @Composable
    private fun getUiTweaks(
        playerPreferences: PlayerPreferences
    ): TweakGroup {
        return TweakGroup(
            title = stringResource(LocaleR.string.user_interface),
            tweaks = persistentListOf(
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.reverse_player_time),
                    description = stringResource(LocaleR.string.reverse_player_time_desc),
                    value = remember { mutableStateOf(playerPreferences.isDurationReversed) },
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(isDurationReversed = it)
                        }
                    }
                ),
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.pip_mode),
                    description = stringResource(LocaleR.string.pip_mode_desc),
                    value = remember { mutableStateOf(playerPreferences.isPiPModeEnabled) },
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(isPiPModeEnabled = it)
                        }
                    }
                ),

            )
        )
    }

    @Composable
    private fun getAudioTweaks(
        playerPreferences: PlayerPreferences
    ): TweakGroup {
        val languages = remember {
            Locale.getAvailableLocales()
                .distinctBy { it.language }
                .associate {
                    it.language to "${it.displayLanguage} [${it.language}]"
                }
                .toImmutableMap()
        }

        val useVolumeBooster = remember { mutableStateOf(playerPreferences.isUsingVolumeBoost) }

        return TweakGroup(
            title = stringResource(LocaleR.string.audio),
            tweaks = persistentListOf(
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.volume_booster),
                    description = stringResource(LocaleR.string.volume_booster_desc),
                    value = useVolumeBooster,
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(isPiPModeEnabled = it)
                        }
                    },
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.preferred_audio_language),
                    description = Locale(playerPreferences.audioLanguage).displayLanguage,
                    value = remember { mutableStateOf(playerPreferences.audioLanguage) },
                    options = languages,
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(audioLanguage = it)
                        }
                    }
                )
            )
        )
    }

    @Composable
    private fun getAdvancedTweaks(
        playerPreferences: PlayerPreferences
    ): TweakGroup {
        val context = LocalContext.current

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
                    value = remember { mutableStateOf(playerPreferences.isForcingPlayerRelease) },
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(isForcingPlayerRelease = it)
                        }
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.decoder_priority),
                    description = stringResource(LocaleR.string.decoder_priority_description),
                    value = remember { mutableStateOf(playerPreferences.decoderPriority) },
                    options = availableDecoders,
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(decoderPriority = it)
                        }
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.video_cache_size),
                    description = stringResource(LocaleR.string.video_cache_size_label),
                    value = remember { mutableLongStateOf(playerPreferences.diskCacheSize) },
                    options = getAvailableCacheSizes(context),
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(diskCacheSize = it)
                        }
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.video_buffer_size),
                    description = stringResource(LocaleR.string.video_buffer_size_label),
                    value = remember { mutableLongStateOf(playerPreferences.bufferCacheSize) },
                    options = getAvailableBufferSizes(context),
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(bufferCacheSize = it)
                        }
                    }
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.video_buffer_max_length),
                    description = stringResource(LocaleR.string.video_buffer_max_length_desc),
                    value = remember { mutableLongStateOf(playerPreferences.videoBufferMs) },
                    options = playerBufferLengths,
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(videoBufferMs = it)
                        }
                    }
                )
            )
        )
    }

}