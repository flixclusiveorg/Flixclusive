package com.flixclusive.feature.mobile.player.controls

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.flixclusive.core.ui.player.PlayerSnackbarMessageType
import com.flixclusive.core.ui.player.PlayerUiState
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.locale.UiText
import com.flixclusive.feature.mobile.player.R
import com.flixclusive.feature.mobile.player.controls.dialogs.audio_and_subtitle.PlayerAudioAndSubtitleDialog
import com.flixclusive.feature.mobile.player.controls.dialogs.servers.PlayerServersDialog
import com.flixclusive.feature.mobile.player.controls.dialogs.settings.PlayerSettingsDialog
import com.flixclusive.feature.mobile.player.controls.episodes.EpisodesScreen
import com.flixclusive.feature.mobile.player.controls.gestures.GestureDirection
import com.flixclusive.feature.mobile.player.controls.gestures.SeekerAndSliderGestures
import com.flixclusive.feature.mobile.player.util.rememberBrightnessManager
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.provider.ProviderApi
import com.flixclusive.core.ui.player.R as PlayerR
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun PlayerControls(
    isVisible: Boolean,
    appSettings: AppSettings,
    areControlsLocked: Boolean,
    isDoubleTapping: MutableState<Boolean>,
    isEpisodesSheetOpened: MutableState<Boolean>,
    isAudiosAndSubtitlesDialogOpened: MutableState<Boolean>,
    isPlayerSettingsDialogOpened: MutableState<Boolean>,
    isServersDialogOpened: MutableState<Boolean>,
    watchHistoryItem: WatchHistoryItem?,
    servers: List<Stream>,
    isLastEpisode: Boolean,
    providerApis: List<ProviderApi>,
    availableSeasons: Int?,
    state: PlayerUiState,
    seasonData: Resource<Season?>,
    currentEpisodeSelected: Episode?,
    showControls: (Boolean) -> Unit,
    lockControls: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSnackbarToggle: (UiText, PlayerSnackbarMessageType) -> Unit,
    onSeasonChange: (Int) -> Unit,
    onProviderChange: (String) -> Unit,
    onVideoServerChange: (Int) -> Unit,
    onResizeModeChange: (Int) -> Unit,
    onPanelChange: (Int) -> Unit,
    onEpisodeClick: (Episode?) -> Unit,
    addSubtitle: (Subtitle) -> Unit,
    toggleVideoTimeReverse: () -> Unit,
) {
    val player by rememberLocalPlayerManager()
    val brightnessManager = rememberBrightnessManager()

    val volumeIconId = remember(player.volumeManager.currentVolume) {
        player.volumeManager.run {
            when {
                currentVolumePercentage > 0.8F -> R.drawable.volume_up_black_24dp
                currentVolumePercentage < 0.4F && currentVolumePercentage > 0F -> R.drawable.volume_down_black_24dp
                currentVolumePercentage == 0F -> R.drawable.volume_off_black_24dp
                else -> R.drawable.volume_up_black_24dp
            }
        }
    }

    fun triggerSnackbar(
        message: String,
        @StringRes messageFormat: Int,
        type: PlayerSnackbarMessageType,
    ) {
        onSnackbarToggle(
            UiText.StringResource(messageFormat, message),
            type
        )
    }

    BackHandler {
        if (
            isEpisodesSheetOpened.value
            || isAudiosAndSubtitlesDialogOpened.value
            || isPlayerSettingsDialogOpened.value
            || isServersDialogOpened.value
            || areControlsLocked
        ) {
            isEpisodesSheetOpened.value = false
            isAudiosAndSubtitlesDialogOpened.value = false
            isPlayerSettingsDialogOpened.value = false
            isServersDialogOpened.value = false

            if (areControlsLocked)
                showControls(true)

            return@BackHandler
        }

        onBack()
    }

    LockControls(
        areControlsVisible = isVisible,
        shouldLockControls = areControlsLocked,
        onVisibilityChange = { lockControls(it) },
        showPlaybackControls = showControls
    )

    if (!areControlsLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val isSeeking = remember { mutableStateOf(false) }

            if(!isSeeking.value) {
                // Left gestures
                SeekerAndSliderGestures(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight(0.85F),
                    direction = GestureDirection.Left,
                    isDoubleTapping = isDoubleTapping,
                    areControlsVisible = isVisible,
                    seekerIconId = PlayerR.drawable.round_keyboard_double_arrow_left_24,
                    seekAction = player::seekBack,
                    sliderValue = brightnessManager.currentBrightness,
                    sliderValueRange = 0F..brightnessManager.maxBrightness,
                    sliderIconId = R.drawable.round_wb_sunny_24,
                    slideAction = brightnessManager::setBrightness,
                    showControls = showControls
                )

                // Right gestures
                SeekerAndSliderGestures(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.85F),
                    direction = GestureDirection.Right,
                    isDoubleTapping = isDoubleTapping,
                    areControlsVisible = isVisible,
                    seekerIconId = PlayerR.drawable.round_keyboard_double_arrow_right_24,
                    seekAction = player::seekForward,
                    sliderValue = player.volumeManager.currentVolume,
                    sliderValueRange = 0F..player.volumeManager.maxVolume,
                    sliderIconId = volumeIconId,
                    slideAction = player.volumeManager::setVolume,
                    showControls = showControls
                )
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    if (!isSeeking.value) {
                        TopControls(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .animateEnterExit(
                                    enter = slideInVertically(
                                        initialOffsetY = { -it }
                                    ),
                                    exit = slideOutVertically(
                                        targetOffsetY = { -it }
                                    )
                                ),
                            currentEpisodeSelected = currentEpisodeSelected,
                            onNavigationIconClick = onBack,
                            onServersClick = {
                                isServersDialogOpened.value = true
                            },
                            onPlayerSettingsClick = {
                                isPlayerSettingsDialogOpened.value = true
                            }
                        )

                        CenterControls(
                            modifier = Modifier.align(Alignment.Center),
                            seekIncrementMs = appSettings.preferredSeekAmount,
                            showControls = showControls
                        )
                    }

                    BottomControls(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .animateEnterExit(
                                enter = slideInVertically(
                                    initialOffsetY = { it }
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { it }
                                )
                            ),
                        isSeeking = isSeeking,
                        isPlayerTimeReversed = appSettings.isPlayerTimeReversed,
                        isTvShow = currentEpisodeSelected != null,
                        isLastEpisode = isLastEpisode,
                        showControls = showControls,
                        onAudioAndDisplayClick = {
                            isAudiosAndSubtitlesDialogOpened.value = true
                        },
                        onEpisodesClick = {
                            isEpisodesSheetOpened.value = true

                            player.run {
                                if(isPlaying) {
                                    playWhenReady = true
                                    pause()
                                }
                            }
                        },
                        onNextEpisodeClick = onEpisodeClick,
                        toggleVideoTimeReverse = toggleVideoTimeReverse,
                        onLockClick = {
                            lockControls(true)
                            showControls(true)
                        }
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = isEpisodesSheetOpened.value,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        EpisodesScreen(
            seasonData = seasonData,
            availableSeasons = availableSeasons!!,
            currentEpisodeSelected = currentEpisodeSelected!!,
            watchHistoryItem = watchHistoryItem,
            onEpisodeClick = onEpisodeClick,
            onSeasonChange = onSeasonChange,
            onClose = {
                isEpisodesSheetOpened.value = false

                player.run {
                    if (playWhenReady && !isPlaying) {
                        play()
                    }
                }
            },
        )
    }

    AnimatedVisibility(
        visible = isAudiosAndSubtitlesDialogOpened.value,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        PlayerAudioAndSubtitleDialog(
            showSnackbar = { message, formatter, type ->
                triggerSnackbar(
                    message,
                    formatter,
                    type
                )
            },
            addSubtitle = addSubtitle,
            onDismissSheet = {
                isAudiosAndSubtitlesDialogOpened.value = false
            }
        )
    }

    AnimatedVisibility(
        visible = isPlayerSettingsDialogOpened.value,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        PlayerSettingsDialog(
            state = state,
            showSnackbar = { message, formatter, type ->
                triggerSnackbar(
                    message,
                    formatter,
                    type
                )
            },
            onResizeModeChange = onResizeModeChange,
            onPanelChange = onPanelChange,
            onDismissSheet = {
                isPlayerSettingsDialogOpened.value = false
            }
        )
    }

    AnimatedVisibility(
        visible = isServersDialogOpened.value,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        PlayerServersDialog(
            state = state,
            servers = servers,
            providers = providerApis,
            onProviderChange = { provider ->
                onProviderChange(provider)
                triggerSnackbar(
                    provider,
                    LocaleR.string.provider_snackbar_message,
                    PlayerSnackbarMessageType.Provider
                )
            },
            onVideoServerChange = { i, message ->
                onVideoServerChange(i)
                triggerSnackbar(
                    message,
                    LocaleR.string.server_snackbar_message,
                    PlayerSnackbarMessageType.Server
                )
            },
            onDismissSheet = {
                isServersDialogOpened.value = false
            }
        )
    }
}