package com.flixclusive.feature.mobile.settings.screen.player

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
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
import kotlinx.coroutines.launch
import java.util.Locale
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

internal class PlayerTweakScreen(
    private val viewModel: SettingsViewModel,
) : BaseTweakScreen<PlayerPreferences> {
    override val key = UserPreferences.PLAYER_PREFS_KEY
    override val preferencesAsState: StateFlow<PlayerPreferences> =
        viewModel.getUserPrefsAsState<PlayerPreferences>(key)

    override fun onUpdatePreferences(transform: suspend (t: PlayerPreferences) -> PlayerPreferences) {
        viewModel.updateUserPrefs(key, transform)
    }

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
        val resources = LocalResources.current
        val navigator = LocalScaffoldNavigator.current!!

        val scope = rememberCoroutineScope()

        val formatInSeconds = fun(amount: Long): String {
            return resources.getString(LocaleR.string.n_seconds_format, amount)
        }

        return TweakGroup(
            title = stringResource(LocaleR.string.video),
            tweaks = persistentListOf(
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.resize_mode),
                    description = {
                        playerPreferences().resizeMode.uiText.asString(resources)
                    },
                    value = { playerPreferences().resizeMode },
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(resizeMode = it)
                        }
                    },
                    options = ResizeMode.entries
                        .associateWith { it.uiText.asString(resources) }
                        .toImmutableMap(),
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.preferred_quality),
                    description = {
                        playerPreferences().quality.uiText.asString(resources)
                    },
                    value = { playerPreferences().quality },
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(quality = it)
                        }
                    },
                    options = PlayerQuality.entries
                        .associateWith { it.uiText.asString(resources) }
                        .toImmutableMap(),
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.seek_length_label),
                    description = {
                        val amountInSeconds = playerPreferences().seekAmount / 1000
                        formatInSeconds(amountInSeconds)
                    },
                    value = { playerPreferences().seekAmount },
                    options = persistentMapOf(
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
                    description = { resources.getString(LocaleR.string.subtitles_settings_content_desc) },
                    onClick = {
                        scope.launch {
                            navigator.navigateTo(
                                pane = ListDetailPaneScaffoldRole.Detail,
                                contentKey = UserPreferences.SUBTITLES_PREFS_KEY.name,
                            )
                        }
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getUiTweaks(playerPreferences: () -> PlayerPreferences): TweakGroup {
        val resources = LocalResources.current
        return TweakGroup(
            title = stringResource(LocaleR.string.user_interface),
            tweaks = persistentListOf(
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.reverse_player_time),
                    description = { resources.getString(LocaleR.string.reverse_player_time_desc) },
                    value = { playerPreferences().isDurationReversed },
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(isDurationReversed = it)
                        }
                    },
                ),
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.pip_mode),
                    description = { resources.getString(LocaleR.string.pip_mode_desc) },
                    value = { playerPreferences().isPiPModeEnabled },
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
        val resources = LocalResources.current

        return TweakGroup(
            title = stringResource(LocaleR.string.audio),
            tweaks = persistentListOf(
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.volume_booster),
                    description = { resources.getString(LocaleR.string.volume_booster_desc) },
                    value = { playerPreferences().isUsingVolumeBoost },
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(isUsingVolumeBoost = it)
                        }
                    },
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.preferred_audio_language),
                    description = {
                        Locale
                            .Builder()
                            .setLanguage(playerPreferences().audioLanguage)
                            .build()
                            .displayLanguage
                    },
                    value = { playerPreferences().audioLanguage },
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
        val resources = LocalResources.current
        val context = LocalContext.current

        val availableDecoders = remember {
            DecoderPriority.entries
                .associateWith { it.uiText.asString(resources) }
                .toPersistentMap()
        }

        return TweakGroup(
            title = stringResource(LocaleR.string.advanced),
            tweaks = persistentListOf(
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.decoder_priority),
                    description = { resources.getString(LocaleR.string.decoder_priority_description) },
                    value = { playerPreferences().decoderPriority },
                    options = availableDecoders,
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(decoderPriority = it)
                        }
                    },
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.video_cache_size),
                    description = { resources.getString(LocaleR.string.video_cache_size_label) },
                    value = { playerPreferences().diskCacheSize },
                    options = getAvailableCacheSizes(context),
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(diskCacheSize = it)
                        }
                    },
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.video_buffer_size),
                    description = { resources.getString(LocaleR.string.video_buffer_size_label) },
                    value = { playerPreferences().bufferCacheSize },
                    options = getAvailableBufferSizes(context),
                    onTweaked = {
                        onUpdatePreferences { oldValue ->
                            oldValue.copy(bufferCacheSize = it)
                        }
                    },
                ),
                TweakUI.ListTweak(
                    title = stringResource(LocaleR.string.video_buffer_max_length),
                    description = { resources.getString(LocaleR.string.video_buffer_max_length_desc) },
                    value = { playerPreferences().videoBufferMs },
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
