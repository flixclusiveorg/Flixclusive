package com.flixclusive.presentation.mobile.screens.player.controls

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.TrackSelectionParameters
import com.flixclusive.R
import com.flixclusive.domain.common.Resource
import com.flixclusive_provider.models.common.VideoData
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.presentation.common.PlayerUiState
import com.flixclusive.presentation.common.PlayerUiState.Companion.toPlaybackSpeed
import com.flixclusive.presentation.mobile.screens.player.PlayerSnackbarMessageType
import com.flixclusive.presentation.mobile.screens.player.controls.episodes_sheet.MoreEpisodesSheet
import com.flixclusive.presentation.mobile.screens.player.controls.gestures.AnimatedSeeker
import com.flixclusive.presentation.mobile.screens.player.controls.qualities_and_subtitles_sheet.QualitiesAndSubtitlesSheet
import com.flixclusive.presentation.mobile.screens.player.controls.video_settings_dialog.VideoSettingsDialog
import com.flixclusive.presentation.utils.PlayerUiUtils.LocalPlayer
import com.flixclusive.common.UiText

const val SEEK_ANIMATION_DELAY = 450L

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerControls(
    visibilityProvider: () -> Boolean,
    areControlsLocked: Boolean,
    watchHistoryItem: WatchHistoryItem?,
    videoData: VideoData,
    availableSeasons: Int?,
    isLastEpisode: Boolean,
    stateProvider: () -> PlayerUiState,
    videoQualities: List<String>,
    seasonDataProvider: () -> Resource<Season>?,
    currentEpisodeSelected: TMDBEpisode?,
    onBrightnessChange: (Float) -> Unit,
    showControls: (Boolean) -> Unit,
    toggleControlLock: (Boolean) -> Unit,
    onBack: () -> Unit,
    onPauseToggle: () -> Unit,
    onSnackbarToggle: (String, PlayerSnackbarMessageType) -> Unit,
    onSeasonChange: (Int) -> Unit,
    onSubtitleChange: (Int, TrackSelectionParameters) -> TrackSelectionParameters,
    onVideoQualityChange: (Int, TrackSelectionParameters) -> TrackSelectionParameters?,
    onVideoServerChange: (Int) -> Unit,
    onPlaybackSpeedChange: (Int) -> Unit,
    onEpisodeClick: (TMDBEpisode?) -> Unit,
) {
    val context = LocalContext.current
    val player = LocalPlayer.current

    val isVisible by rememberUpdatedState(visibilityProvider())
    val state by rememberUpdatedState(stateProvider())
    val seasonData by rememberUpdatedState(seasonDataProvider())

    var shouldOpenEpisodesSheet by remember { mutableStateOf(false) }
    var shouldOpenQualitiesAndSubtitles by remember { mutableStateOf(false) }
    var shouldOpenVideoSettingsDialog by remember { mutableStateOf(false) }

    fun triggerSnackbar(
        message: String,
        @StringRes messageFormat: Int,
        type: PlayerSnackbarMessageType
    ) {
        onSnackbarToggle(
            String.format(UiText.StringResource(messageFormat).asString(context), message),
            type
        )
    }

    BackHandler {
        if (shouldOpenEpisodesSheet || shouldOpenQualitiesAndSubtitles || areControlsLocked) {
            shouldOpenEpisodesSheet = false
            shouldOpenQualitiesAndSubtitles = false
            showControls(true)
            return@BackHandler
        }

        onBack()
    }

    LockControls(
        areControlsVisible = isVisible,
        shouldLockControls = areControlsLocked,
        onVisibilityChange = { toggleControlLock(it) },
        showPlaybackControls = showControls
    )

    if (!areControlsLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Left clicker
            AnimatedSeeker(
                modifier = Modifier
                    .align(Alignment.CenterStart),
                areControlsVisible = isVisible,
                iconId = R.drawable.round_keyboard_double_arrow_left_24,
                enterTransition = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 500)
                ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                exitTransition = fadeOut(animationSpec = tween(durationMillis = 500)) + slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(durationMillis = 500)
                ),
                seekAction = { player?.seekBack() },
                showControls = showControls
            )

            // Right clicker
            AnimatedSeeker(
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                areControlsVisible = isVisible,
                iconId = R.drawable.round_keyboard_double_arrow_right_24,
                enterTransition = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(durationMillis = 500)
                ) + fadeIn(animationSpec = tween(durationMillis = 500)),
                exitTransition = fadeOut(animationSpec = tween(durationMillis = 500)) + slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 500)
                ),
                seekAction = { player?.seekForward() },
                showControls = showControls
            )


            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    TopControls(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .animateEnterExit(
                                enter = slideInVertically(
                                    initialOffsetY = { fullHeight: Int ->
                                        -fullHeight
                                    }
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { fullHeight: Int ->
                                        -fullHeight
                                    }
                                )
                            ),
                        onNavigationIconClick = onBack,
                        onVideoSettingsClick = {
                            showControls(false)
                            shouldOpenVideoSettingsDialog = true
                        }
                    )

                    CenterControls(
                        modifier = Modifier.align(Alignment.Center),
                        state = state,
                        onPauseToggle = onPauseToggle,
                        showControls = showControls,
                        onBrightnessChange = onBrightnessChange
                    )

                    BottomControls(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .animateEnterExit(
                                enter = slideInVertically(
                                    initialOffsetY = { fullHeight: Int ->
                                        fullHeight
                                    }
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { fullHeight: Int ->
                                        fullHeight
                                    }
                                )
                            ),
                        state = state,
                        isTvShow = currentEpisodeSelected != null,
                        isLastEpisode = isLastEpisode,
                        showControls = showControls,
                        onQualityAndSubtitleClick = {
                            shouldOpenQualitiesAndSubtitles = true
                            showControls(false)
                        },
                        onMoreVideosClick = {
                            shouldOpenEpisodesSheet = true
                            showControls(false)
                        },
                        onNextEpisodeClick = onEpisodeClick,
                        onLockClick = {
                            toggleControlLock(true)
                            showControls(true)
                        }
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = shouldOpenEpisodesSheet,
        enter = slideInHorizontally(animationSpec = tween(durationMillis = 500)),
        exit = slideOutHorizontally(animationSpec = tween(durationMillis = 500)),
    ) {
        MoreEpisodesSheet(
            seasonData = seasonData!!,
            availableSeasons = availableSeasons!!,
            currentEpisodeSelected = currentEpisodeSelected!!,
            watchHistoryItem = watchHistoryItem,
            onEpisodeClick = onEpisodeClick,
            onSeasonChange = onSeasonChange,
            onDismissSheet = {
                shouldOpenEpisodesSheet = false
                showControls(true)
            },
        )
    }

    AnimatedVisibility(
        visible = shouldOpenQualitiesAndSubtitles,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(durationMillis = 500)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(durationMillis = 500)
        )
    ) {
        QualitiesAndSubtitlesSheet(
            subtitles = videoData.subtitles,
            qualities = videoQualities,
            selectedSubtitle = state.selectedSubtitle,
            selectedQuality = state.selectedQuality,
            onSubtitleChange = { i, message ->
                player?.run {
                    trackSelectionParameters = onSubtitleChange(
                        i,
                        trackSelectionParameters
                    )

                    triggerSnackbar(
                        message,
                        R.string.subtitle_snackbar_message,
                        PlayerSnackbarMessageType.Subtitle
                    )
                }
            },
            onVideoQualityChange = { i, message ->
                player?.run {
                    trackSelectionParameters = onVideoQualityChange(
                        i,
                        trackSelectionParameters
                    ) ?: return@run

                    triggerSnackbar(
                        message,
                        R.string.quality_snackbar_message,
                        PlayerSnackbarMessageType.Quality
                    )
                }
            },
            onDismissSheet = {
                shouldOpenQualitiesAndSubtitles = false
                showControls(true)
            }
        )
    }

    AnimatedVisibility(
        visible = shouldOpenVideoSettingsDialog,
        enter = scaleIn(),
        exit = scaleOut()
    ) {
        VideoSettingsDialog(
            state = state,
            servers = videoData.servers ?: emptyList(),
            onPlaybackSpeedChange = {
                val playbackSpeed = it.toPlaybackSpeed()

                onPlaybackSpeedChange(it)
                player?.playbackParameters = PlaybackParameters(playbackSpeed)

                triggerSnackbar(
                    playbackSpeed.toString(),
                    R.string.playback_speed_snackbar_message,
                    PlayerSnackbarMessageType.PlaybackSpeed
                )
            },
            onVideoServerChange = { i, message ->
                onVideoServerChange(i)
                triggerSnackbar(
                    message,
                    R.string.server_snackbar_message,
                    PlayerSnackbarMessageType.Server
                )
            },
            onDismissSheet = {
                shouldOpenVideoSettingsDialog = false
                showControls(true)
            }
        )
    }
}