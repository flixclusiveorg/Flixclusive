package com.flixclusive.presentation.mobile.screens.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import com.flixclusive.domain.model.SourceDataState
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.presentation.common.composables.SourceDataDialog
import com.flixclusive.presentation.common.player.PLAYER_CONTROL_VISIBILITY_TIMEOUT
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.AudioFocusManager
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.LifecycleAwarePlayer
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.LocalPlayer
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.ObserveNewLinksAndSubtitles
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.ObservePlayerTimer
import com.flixclusive.presentation.common.viewmodels.player.PlayerScreenNavArgs
import com.flixclusive.presentation.mobile.common.ListenKeyEvents
import com.flixclusive.presentation.mobile.main.MainActivity
import com.flixclusive.presentation.mobile.main.MainMobileSharedViewModel
import com.flixclusive.presentation.mobile.screens.player.controls.PlayerControls
import com.flixclusive.presentation.mobile.screens.player.utils.PlayerPiPUtils.updatePiPParams
import com.flixclusive.presentation.mobile.screens.player.utils.PlayerPipReceiver
import com.flixclusive.presentation.mobile.screens.player.utils.getActivity
import com.flixclusive.presentation.mobile.screens.player.utils.percentOfVolume
import com.flixclusive.presentation.mobile.screens.player.utils.setBrightness
import com.flixclusive.presentation.mobile.screens.player.utils.toggleSystemBars
import com.flixclusive.presentation.utils.FormatterUtils.formatPlayerTitle
import com.flixclusive.presentation.utils.ModifierUtils.noIndicationClickable
import com.flixclusive.utils.LoggerUtils.debugLog
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlin.math.roundToInt


const val ACTION_PIP_CONTROL = "player_pip_control"
const val PLAYER_PIP_EVENT = "player_pip_event"

const val DEFAULT_PLAYER_SEEK_AMOUNT = 10000L

@OptIn(UnstableApi::class)
@Destination(
    style = PlayerScreenTransition::class,
    navArgsDelegate = PlayerScreenNavArgs::class
)
@Composable
fun PlayerScreen(
    navigator: DestinationsNavigator,
    sharedViewModel: MainMobileSharedViewModel,
    args: PlayerScreenNavArgs,
) {
    val viewModel: PlayerViewModel = hiltViewModel()

    val context = LocalContext.current.getActivity<MainActivity>()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val appUiState by sharedViewModel.uiState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val watchHistoryItem by viewModel.watchHistoryItem.collectAsStateWithLifecycle()

    val sourceData = viewModel.sourceData
    val seasonData by viewModel.season.collectAsStateWithLifecycle()
    val currentSelectedEpisode by viewModel.currentSelectedEpisode.collectAsStateWithLifecycle()

    val currentPlayerTitle = remember(currentSelectedEpisode) {
        formatPlayerTitle(args.film, currentSelectedEpisode)
    }
    var controlTimeoutVisibility by remember {
        mutableIntStateOf(PLAYER_CONTROL_VISIBILITY_TIMEOUT)
    }
    val isEpisodesSheetOpened = remember {
        mutableStateOf(false)
    }
    val isAudiosAndSubtitlesDialogOpened = remember {
        mutableStateOf(false)
    }
    val isPlayerSettingsDialogOpened = remember {
        mutableStateOf(false)
    }
    val isServersDialogOpened = remember {
        mutableStateOf(false)
    }
    val snackbarBottomPadding by animateDpAsState(
        targetValue = if (viewModel.areControlsVisible) 100.dp else 0.dp,
        label = ""
    )

    fun showControls(isShowing: Boolean) {
        val areSomeSheetsOpened = isEpisodesSheetOpened.value
                || isPlayerSettingsDialogOpened.value
                || isAudiosAndSubtitlesDialogOpened.value
                || isServersDialogOpened.value

        val isLoading = (!viewModel.player.hasBeenInitialized
                || !viewModel.player.isPlaying
                || viewModel.player.playbackState == STATE_BUFFERING
                || viewModel.player.playbackState == STATE_ENDED) && !viewModel.areControlsLocked

        controlTimeoutVisibility = if (!isShowing || areSomeSheetsOpened) {
            0
        } else if (isLoading) {
            Int.MAX_VALUE
        } else {
            PLAYER_CONTROL_VISIBILITY_TIMEOUT
        }
    }

    fun onEpisodeClick(episode: TMDBEpisode? = null) {
        viewModel.onEpisodeClick(episodeToWatch = episode)
    }

    CompositionLocalProvider(LocalPlayer provides viewModel.player) {
        PlayerPipReceiver(
            action = ACTION_PIP_CONTROL,
            onReceive = { broadcastIntent ->
                if (
                    SDK_INT >= Build.VERSION_CODES.O
                    && broadcastIntent?.action == ACTION_PIP_CONTROL
                ) {
                    val event = broadcastIntent.getIntExtra(PLAYER_PIP_EVENT, -1)

                    if (event == -1)
                        return@PlayerPipReceiver

                    viewModel.player.handleBroadcastEvents(event)
                }
            }
        )

        /**
         * Check if activity is forced to go to
         * pip mode, then update the ui to pip view
         * based on the current player ui state
         * */
        LaunchedEffect(
            appUiState.isInPipMode,
            viewModel.player.isPlaying,
            viewModel.player.playbackState
        ) {
            if (appUiState.isInPipMode && SDK_INT >= Build.VERSION_CODES.O) {
                debugLog("Entering PiP mode...")
                context.updatePiPParams(
                    isPlaying = viewModel.player.isPlaying,
                    hasEnded = viewModel.player.playbackState == STATE_ENDED,
                    preferredSeekIncrement = appSettings.preferredSeekAmount
                )
            }
        }

        LaunchedEffect(viewModel.player.playbackState) {
            when (viewModel.player.playbackState) {
                STATE_READY -> {
                    // Find film's audio language
                    viewModel.run {
                        if (!player.hasBeenInitialized) {
                            player.onAudioChange(language = film.language)
                        }
                    }
                }

                STATE_ENDED -> {
                    if (!viewModel.isLastEpisode && !appUiState.isInPipMode) {
                        viewModel.player.run {
                            viewModel.updateWatchHistory(
                                currentTime = currentPosition,
                                duration = duration
                            )
                        }
                        onEpisodeClick()
                    }
                }
            }
        }

        /**
         *
         * Initialize every view state for the player
         * and put them back on the default state if
         * user is leaving the player
         * */
        DisposableEffect(Unit) {
            sharedViewModel.setIsInPlayer(state = true)
            sharedViewModel.toggleBottomBar(isVisible = false)
            val originalOrientation = context.requestedOrientation

            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            context.toggleSystemBars(isVisible = false)

            // Initialize brightness
            val userDefaultBrightnessLevel = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                -1
            ) / 255F

            viewModel.updateScreenBrightness(userDefaultBrightnessLevel)

            // Initialize volume level
            viewModel.updateVolume(context.percentOfVolume())

            onDispose {
                context.requestedOrientation = originalOrientation
                context.setBrightness(userDefaultBrightnessLevel)
                context.toggleSystemBars(isVisible = true)
                sharedViewModel.toggleBottomBar(isVisible = true)
                sharedViewModel.setIsInPlayer(state = false)
            }
        }

        ListenKeyEvents { code, _ ->
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            when (code) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    val newVolume = currentVolume + 1F

                    audioManager.setStreamVolume(
                        /* streamType = */ AudioManager.STREAM_MUSIC,
                        /* index = */ newVolume.roundToInt(),
                        /* flags = */ 0
                    )
                    viewModel.updateVolume(newVolume / maxVolume)
                    true
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val newVolume = currentVolume - 1F
                    audioManager.setStreamVolume(
                        /* streamType = */ AudioManager.STREAM_MUSIC,
                        /* index = */ newVolume.roundToInt(),
                        /* flags = */ 0
                    )
                    viewModel.updateVolume(newVolume / maxVolume)
                    true
                }

                else -> false
            }
        }

        LaunchedEffect(controlTimeoutVisibility) {
            if (controlTimeoutVisibility > 0) {
                viewModel.areControlsVisible = true
                delay(1000)
                controlTimeoutVisibility -= 1
            } else viewModel.areControlsVisible = false
        }

        // Re-prepare the player if provider data has changed
        ObserveNewLinksAndSubtitles(
            selectedSourceLink = uiState.selectedSourceLink,
            currentPlayerTitle = currentPlayerTitle,
            newLinks = sourceData.cachedLinks,
            newSubtitles = sourceData.cachedSubtitles,
            getSavedTimeForCurrentSourceData = {
                viewModel.getSavedTimeForSourceData(currentSelectedEpisode).first
            }
        )

        /**
         *
         * Purpose (unless interacted):
         * Always show controls when player is paused.
         * Show it when player hasn't been initialized.
         * Don't show it if its locked and its buffering.
         * Show controls when buffering
         *
         * See (CTRL+F): [showControls]
         *
         * */
        LaunchedEffect(
            viewModel.player.hasBeenInitialized,
            viewModel.areControlsLocked,
            viewModel.player.isPlaying,
            viewModel.player.playbackState,
            isEpisodesSheetOpened.value,
            isPlayerSettingsDialogOpened.value,
            isAudiosAndSubtitlesDialogOpened.value,
            isServersDialogOpened.value,
        ) {
            showControls(true)
        }

        ObservePlayerTimer(
            isTvShow = args.film.filmType == FilmType.TV_SHOW,
            isLastEpisode = viewModel.isLastEpisode,
            isInPipMode = appUiState.isInPipMode,
            showSnackbar = viewModel::showSnackbar,
            onQueueNextEpisode = viewModel::onQueueNextEpisode
        )

        AudioFocusManager(
            activity = context,
            preferredSeekAmount = appSettings.preferredSeekAmount
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            LifecycleAwarePlayer(
                isInPipMode = appUiState.isInPipMode,
                areControlsVisible = viewModel.areControlsVisible && !viewModel.areControlsLocked,
                resizeMode = uiState.selectedResizeMode,
                releaseOnStop = appSettings.shouldReleasePlayer,
                onInitialize = {
                    viewModel.run {
                        val (currentPosition, _) = viewModel.getSavedTimeForSourceData(
                            currentSelectedEpisode
                        )

                        player.initialize()
                        sourceData.run {
                            val getPossibleSourceLink = cachedLinks
                                .getOrNull(uiState.selectedSourceLink)
                                ?: cachedLinks.getOrNull(0)

                            getPossibleSourceLink?.let {
                                player.prepare(
                                    link = it,
                                    title = currentPlayerTitle,
                                    subtitles = cachedSubtitles,
                                    initialPlaybackPosition = currentPosition
                                )
                            }
                        }
                    }
                },
                onRelease = {
                    viewModel.player.run {
                        viewModel.updateWatchHistory(
                            currentTime = currentPosition,
                            duration = duration
                        )

                        release()
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .noIndicationClickable {
                        showControls(!viewModel.areControlsVisible)
                    }
            )

            PlayerControls(
                visibilityProvider = {
                    viewModel.areControlsVisible &&
                            !appUiState.isInPipMode
                },
                appSettings = appSettings,
                areControlsLocked = viewModel.areControlsLocked,
                isEpisodesSheetOpened = isEpisodesSheetOpened,
                isAudiosAndSubtitlesDialogOpened = isAudiosAndSubtitlesDialogOpened,
                servers = sourceData.cachedLinks,
                isPlayerSettingsDialogOpened = isPlayerSettingsDialogOpened,
                isServersDialogOpened = isServersDialogOpened,
                watchHistoryItem = watchHistoryItem,
                sourceProviders = viewModel.sourceProviders,
                availableSeasons = (args.film as? TvShow)?.totalSeasons,
                currentEpisodeSelected = currentSelectedEpisode,
                isLastEpisode = viewModel.isLastEpisode,
                stateProvider = { uiState },
                seasonDataProvider = { seasonData },
                onBack = navigator::navigateUp,
                showControls = { showControls(it) },
                toggleControlLock = { viewModel.areControlsLocked = it },
                onBrightnessChange = {
                    context.setBrightness(it)
                    viewModel.updateScreenBrightness(it)
                },
                onVolumeChange = {
                    viewModel.player.run {
                        val newVolume = (it * 15).roundToInt()

                        setDeviceVolume(newVolume)
                        viewModel.updateVolume(it)
                    }
                },
                onSnackbarToggle = viewModel::showSnackbar,
                onSeasonChange = viewModel::onSeasonChange,
                onVideoServerChange = viewModel::onServerChange,
                onSourceChange = viewModel::onProviderChange,
                onResizeModeChange = viewModel::onResizeModeChange,
                onPanelChange = viewModel::onPanelChange,
                onEpisodeClick = {
                    viewModel.run {
                        updateWatchHistory(
                            currentTime = player.currentPosition,
                            duration = player.duration
                        )

                        onEpisodeClick(it)
                    }
                },
                toggleVideoTimeReverse = viewModel::toggleVideoTimeReverse,
            )

            if (!appUiState.isInPipMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = snackbarBottomPadding)
                ) {
                    LazyColumn {
                        items(
                            items = viewModel.snackbarQueue,
                            key = { data -> data.type }
                        ) { data ->
                            PlayerSnackbar(
                                messageData = data,
                                onDismissMessage = {
                                    viewModel.removeSnackbar(data)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (dialogState !is SourceDataState.Idle) {
        LaunchedEffect(Unit) {
            viewModel.player.run {
                if (isPlaying) {
                    pause()
                    playWhenReady = true
                }
            }
        }

        SourceDataDialog(
            state = dialogState,
            onConsumeDialog = {
                viewModel.onConsumePlayerDialog()

                if (viewModel.player.playWhenReady) {
                    viewModel.player.play()
                }
            }
        )
    }
}