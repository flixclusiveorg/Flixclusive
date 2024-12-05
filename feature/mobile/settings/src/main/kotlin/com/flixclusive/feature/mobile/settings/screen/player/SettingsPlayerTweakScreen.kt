package com.flixclusive.feature.mobile.settings.screen.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.flixclusive.feature.mobile.settings.SettingsItem
import com.flixclusive.feature.mobile.settings.Tweak
import com.flixclusive.feature.mobile.settings.TweakGroup
import com.flixclusive.feature.mobile.settings.TweakUI
import com.flixclusive.feature.mobile.settings.screen.BaseTweakScreen
import com.flixclusive.feature.mobile.settings.screen.player.PlayerAdvancedValues.getAvailableBufferSizes
import com.flixclusive.feature.mobile.settings.screen.player.PlayerAdvancedValues.getAvailableCacheSizes
import com.flixclusive.feature.mobile.settings.screen.player.PlayerAdvancedValues.playerBufferLengths
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.KEY_PLAYER_DISK_CACHE_DIALOG
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.rememberAppSettingsChanger
import com.flixclusive.feature.mobile.settings.util.LocalProviderHelper.rememberLocalAppSettings
import com.flixclusive.model.datastore.player.DecoderPriority
import com.flixclusive.model.datastore.player.PlayerQuality
import com.flixclusive.model.datastore.player.ResizeMode
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import com.flixclusive.core.locale.R as LocaleR

object SettingsPlayerTweakScreen : BaseTweakScreen {
    @Composable
    @ReadOnlyComposable
    override fun getTitle()
        = stringResource(LocaleR.string.player)

    @Composable
    @ReadOnlyComposable
    override fun getDescription()
        = stringResource(LocaleR.string.player_settings_content_desc)

    @Composable
    override fun getTweaks(): List<Tweak> {
        return listOf(
            getVideoTweaks(),
            getAdvancedTweaks(),
            getSubtitleTweaks(),
            getAudioTweaks(),
            // getNetworkTweaks()
        )
    }

    @Composable
    override fun Content() {
        TODO("Not yet implemented")
    }

    @Composable
    private fun getVideoTweaks(): TweakGroup {
        val context = LocalContext.current
        val appSettings by rememberLocalAppSettings()
        val onTweaked by rememberAppSettingsChanger()

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
                    value = remember { mutableIntStateOf(appSettings.preferredResizeMode) },
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
                )
            )
        )
    }

    @Composable
    private fun getAdvancedTweaks(): TweakGroup {
        val context = LocalContext.current

        val appSettings by rememberLocalAppSettings()
        val onTweaked by rememberAppSettingsChanger()

        val availableDecoders = remember {
            DecoderPriority.entries
                .associateWith { it.toUiText().asString(context) }
                .toPersistentMap()
        }
        SettingsItem(
            title = stringResource(LocaleR.string.video_cache_size),
            description = stringResource(LocaleR.string.video_cache_size_label),
            dialogKey = KEY_PLAYER_DISK_CACHE_DIALOG,
        )

        return TweakGroup(
            title = stringResource(LocaleR.string.advanced),
            tweaks = persistentListOf(
                TweakUI.SwitchTweak(
                    title = stringResource(LocaleR.string.release_player),
                    description = stringResource(LocaleR.string.release_player_desc),
                    value = remember { mutableStateOf(appSettings.shouldReleasePlayer) },
                    onTweaked = {
                        onTweaked(
                            appSettings.copy(isUsingVolumeBoost = !appSettings.isUsingVolumeBoost)
                        )
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

    @Composable
    private fun getSubtitleTweaks(): TweakGroup {
        TODO("Support subtitle tweaks on player")
    }

    @Composable
    private fun getAudioTweaks(): TweakGroup {
        TODO("Support audio tweaks on player")
    }

    @Composable
    private fun getNetworkTweaks(): TweakGroup {
        TODO("Support network tweaks on player")
    }

}