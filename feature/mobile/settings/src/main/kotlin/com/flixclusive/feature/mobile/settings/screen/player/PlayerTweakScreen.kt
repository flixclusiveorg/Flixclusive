package com.flixclusive.feature.mobile.settings.screen.player

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.datastore.model.user.player.DecoderPriority
import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.core.datastore.model.user.player.ResizeMode
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.SettingsViewModel
import com.flixclusive.feature.mobile.settings.util.LocalScaffoldNavigator
import com.flixclusive.feature.mobile.settings.util.uiText
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

internal class PlayerTweakScreen(
    private val viewModel: SettingsViewModel,
) : BaseTweakScreen<PlayerPreferences> {
    override val key = UserPreferences.PLAYER_PREFS_KEY
    override val preferencesAsState: StateFlow<PlayerPreferences> =
        viewModel.getUserPrefsAsState<PlayerPreferences>(key)

    override suspend fun onUpdatePreferences(transform: suspend (t: PlayerPreferences) -> PlayerPreferences): Boolean =
        viewModel.updateUserPrefs(key, transform)

    @Composable
    @ReadOnlyComposable
    override fun getTitle() = stringResource(LocaleR.string.player)

    @Composable
    @ReadOnlyComposable
    override fun getDescription() = stringResource(LocaleR.string.player_settings_content_desc)

    @Composable
    override fun getIconPainter() = painterResource(UiCommonR.drawable.play_outline_circle)

    @Composable
    override fun getTweaks(): List<Tweak> {
        val playerPreferences = preferencesAsState.collectAsStateWithLifecycle()

        return listOf(
            getGeneralTweaks { playerPreferences.value },
            getAudioTweaks { playerPreferences.value },
            getAdvancedTweaks { playerPreferences.value },
            getUiTweaks { playerPreferences.value },
        )
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Composable
    private fun getGeneralTweaks(playerPreferences: () -> PlayerPreferences): TweakGroup {
        val context = LocalContext.current
        val navigator = LocalScaffoldNavigator.current!!

        val formatInSeconds = fun(amount: Long): String {
            return context.getString(LocaleR.string.n_seconds_format, amount)
        }

        val selectedSeekAmount = remember { mutableLongStateOf(playerPreferences().seekAmount) }
        val selectedResizeMode = remember { mutableStateOf(playerPreferences().resizeMode) }
        val selectedQuality = remember { mutableStateOf(playerPreferences().quality) }

        return TweakGroup(
            title = stringResource(LocaleR.string.video),
            tweaks =
                persistentListOf(
                    TweakUI.ListTweak(
                        title = stringResource(LocaleR.string.resize_mode),
                        descriptionProvider = {
                            selectedResizeMode.uiText.asString(context)
                        },
                        value = selectedResizeMode,
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(resizeMode = it)
                            }
                        },
                        options = ResizeMode.entries
                            .associateWith { it.uiText.asString(context) }
                            .toImmutableMap(),
                    ),
                    TweakUI.ListTweak(
                        title = stringResource(LocaleR.string.preferred_quality),
                        descriptionProvider = { selectedQuality.value.uiText.asString(context) },
                        value = selectedQuality,
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(quality = it)
                            }
                        },
                        options = PlayerQuality.entries
                            .associateWith { it.uiText.asString(context) }
                            .toImmutableMap(),
                    ),
                    TweakUI.ListTweak(
                        title = stringResource(LocaleR.string.seek_length_label),
                        descriptionProvider = {
                            val amountInSeconds = selectedSeekAmount.longValue / 1000
                            formatInSeconds(amountInSeconds)
                        },
                        value = selectedSeekAmount,
                        options =
                            persistentMapOf(
                                5000L to formatInSeconds(5),
                                10000L to formatInSeconds(10),
                                30000L to formatInSeconds(30),
                            ),
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(seekAmount = it)
                            }
                        },
                    ),
                    TweakUI.ClickableTweak(
                        title = stringResource(LocaleR.string.subtitle),
                        descriptionProvider = { context.getString(LocaleR.string.subtitles_settings_content_desc) },
                        onClick = {
                            navigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                content = UserPreferences.SUBTITLES_PREFS_KEY.name,
                            )
                        },
                    ),
                ),
        )
    }

    @Composable
    private fun getUiTweaks(playerPreferences: () -> PlayerPreferences): TweakGroup {
        val context = LocalContext.current
        return TweakGroup(
            title = stringResource(LocaleR.string.user_interface),
            tweaks =
                persistentListOf(
                    TweakUI.SwitchTweak(
                        title = stringResource(LocaleR.string.reverse_player_time),
                        descriptionProvider = { context.getString(LocaleR.string.reverse_player_time_desc) },
                        value = remember { mutableStateOf(playerPreferences().isDurationReversed) },
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(isDurationReversed = it)
                            }
                        },
                    ),
                    TweakUI.SwitchTweak(
                        title = stringResource(LocaleR.string.pip_mode),
                        descriptionProvider = { context.getString(LocaleR.string.pip_mode_desc) },
                        value = remember { mutableStateOf(playerPreferences().isPiPModeEnabled) },
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(isPiPModeEnabled = it)
                            }
                        },
                    ),
                ),
        )
    }

    @Composable
    private fun getAudioTweaks(playerPreferences: () -> PlayerPreferences): TweakGroup {
        val context = LocalContext.current

        val selectedAudioLanguage = remember { mutableStateOf(playerPreferences().audioLanguage) }

        return TweakGroup(
            title = stringResource(LocaleR.string.audio),
            tweaks =
                persistentListOf(
                    TweakUI.SwitchTweak(
                        title = stringResource(LocaleR.string.volume_booster),
                        descriptionProvider = { context.getString(LocaleR.string.volume_booster_desc) },
                        value = remember { mutableStateOf(playerPreferences().isUsingVolumeBoost) },
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(isPiPModeEnabled = it)
                            }
                        },
                    ),
                    TweakUI.ListTweak(
                        title = stringResource(LocaleR.string.preferred_audio_language),
                        descriptionProvider = {
                            Locale
                                .Builder()
                                .setLanguage(selectedAudioLanguage.value)
                                .build()
                                .displayLanguage
                        },
                        value = selectedAudioLanguage,
                        options = Locale
                            .getAvailableLocales()
                            .distinctBy { it.language }
                            .associate {
                                it.language to "${it.displayLanguage} [${it.language}]"
                            }.toImmutableMap(),
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(audioLanguage = it)
                            }
                        },
                    ),
                ),
        )
    }

    @Composable
    private fun getAdvancedTweaks(playerPreferences: () -> PlayerPreferences): TweakGroup {
        val context = LocalContext.current

        val availableDecoders =
            remember {
                DecoderPriority.entries
                    .associateWith { it.uiText.asString(context) }
                    .toPersistentMap()
            }

        return TweakGroup(
            title = stringResource(LocaleR.string.advanced),
            tweaks =
                persistentListOf(
                    TweakUI.SwitchTweak(
                        title = stringResource(LocaleR.string.release_player),
                        descriptionProvider = { context.getString(LocaleR.string.release_player_desc) },
                        value = remember { mutableStateOf(playerPreferences().isForcingPlayerRelease) },
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(isForcingPlayerRelease = it)
                            }
                        },
                    ),
                    TweakUI.ListTweak(
                        title = stringResource(LocaleR.string.decoder_priority),
                        descriptionProvider = { context.getString(LocaleR.string.decoder_priority_description) },
                        value = remember { mutableStateOf(playerPreferences().decoderPriority) },
                        options = availableDecoders,
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(decoderPriority = it)
                            }
                        },
                    ),
                    TweakUI.ListTweak(
                        title = stringResource(LocaleR.string.video_cache_size),
                        descriptionProvider = { context.getString(LocaleR.string.video_cache_size_label) },
                        value = remember { mutableLongStateOf(playerPreferences().diskCacheSize) },
                        options = getAvailableCacheSizes(context),
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(diskCacheSize = it)
                            }
                        },
                    ),
                    TweakUI.ListTweak(
                        title = stringResource(LocaleR.string.video_buffer_size),
                        descriptionProvider = { context.getString(LocaleR.string.video_buffer_size_label) },
                        value = remember { mutableLongStateOf(playerPreferences().bufferCacheSize) },
                        options = getAvailableBufferSizes(context),
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(bufferCacheSize = it)
                            }
                        },
                    ),
                    TweakUI.ListTweak(
                        title = stringResource(LocaleR.string.video_buffer_max_length),
                        descriptionProvider = { context.getString(LocaleR.string.video_buffer_max_length_desc) },
                        value = remember { mutableLongStateOf(playerPreferences().videoBufferMs) },
                        options = playerBufferLengths,
                        onTweaked = {
                            onUpdatePreferences { oldValue ->
                                oldValue.copy(videoBufferMs = it)
                            }
                        },
                    ),
                ),
        )
    }
}
