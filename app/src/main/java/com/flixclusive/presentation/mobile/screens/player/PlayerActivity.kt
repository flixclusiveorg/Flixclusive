package com.flixclusive.presentation.mobile.screens.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.utils.WatchHistoryUtils.areThereLessThan10SecondsLeftToWatch
import com.flixclusive.presentation.mobile.common.composables.film.dialog_content.VideoPlayerDialog
import com.flixclusive.presentation.mobile.screens.player.controls.PlayerControls
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import com.flixclusive.presentation.utils.PlayerUiUtils.LifecycleAwarePlayer
import com.flixclusive.presentation.utils.PlayerUiUtils.LocalPlayer
import com.flixclusive.presentation.utils.PlayerUiUtils.PLAYER_CONTROL_VISIBILITY_TIMEOUT
import com.flixclusive.presentation.utils.PlayerUiUtils.initializePlayer
import com.flixclusive.presentation.utils.PlayerUiUtils.rePrepare
import com.flixclusive_provider.models.common.VideoData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

const val PLAYER_SEEK_BACK_INCREMENT = 5000L
const val PLAYER_SEEK_FORWARD_INCREMENT = 10000L
const val WATCH_HISTORY_ITEM = "watch_history_item"
const val SEASON_COUNT = "season_count"
const val EPISODE_SELECTED = "episode_selected"
const val VIDEO_DATA = "video_data"

@Suppress("DEPRECATION")
@AndroidEntryPoint
@UnstableApi
class PlayerActivity : ComponentActivity() {
    companion object {
        fun Context.startPlayer(
            videoData: VideoData?,
            watchHistoryItem: WatchHistoryItem? = null,
            seasonCount: Int? = null,
            episodeSelected: TMDBEpisode? = null,
        ) {
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(VIDEO_DATA, videoData)

                if (watchHistoryItem != null)
                    putExtra(WATCH_HISTORY_ITEM, watchHistoryItem)
                if (seasonCount != null)
                    putExtra(SEASON_COUNT, seasonCount)
                if (episodeSelected != null)
                    putExtra(EPISODE_SELECTED, episodeSelected)
            }
            this.startActivity(intent)
        }
    }

    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private lateinit var afChangeListener: AudioManager.OnAudioFocusChangeListener

    private var resumeOnFocusGain = false
    private var shouldPlay = true
    private var playbackDelayed = false
    private var playbackNowAuthorized = false
    private val focusLock = Any()

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).run {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        setContent {
            FlixclusiveMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    var playerTimeUpdaterJob: Job? by remember { mutableStateOf(null) }

                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
                    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
                    val watchHistoryItem by viewModel.watchHistoryItem.collectAsStateWithLifecycle()
                    val seasonData by viewModel.season.collectAsStateWithLifecycle()
                    val seasonCount by viewModel.seasonCount.collectAsStateWithLifecycle()
                    val videoData by viewModel.videoData.collectAsStateWithLifecycle()
                    val currentSelectedEpisode by viewModel.currentSelectedEpisode.collectAsStateWithLifecycle()

                    var currentMediaId by remember { mutableStateOf(videoData.mediaId) }
                    var currentSource by remember { mutableStateOf(videoData.source) }
                    val availableQualities =
                        remember(viewModel.availableQualities.size) { viewModel.availableQualities }
                    val subtitlesList =
                        remember(videoData.subtitles.first().url) { viewModel.availableSubtitles }

                    var mediaSession: MediaSession? by remember {
                        mutableStateOf(
                            context.initializePlayer(
                                currentSource,
                                videoData.title,
                                subtitlesList,
                                uiState.currentTime,
                                uiState.playWhenReady
                            )
                        )
                    }

                    // Re-prepare the player if video data has changed
                    LaunchedEffect(videoData.source, uiState.totalDuration) {
                        val currentAutoAdaptiveSource = videoData.source
                        val isNewServer =
                            !currentAutoAdaptiveSource.equals(currentSource, ignoreCase = true)
                            && videoData.mediaId == currentMediaId

                        val isNewData = videoData.mediaId != currentMediaId

                        if(isNewData) {
                            async { viewModel.initialize() }.await()
                            currentMediaId = videoData.mediaId
                        }

                        if (isNewServer || isNewData) {
                            currentSource = videoData.source
                            mediaSession?.player?.rePrepare(
                                currentSource,
                                videoData,
                                subtitlesList,
                                uiState.currentTime,
                                uiState.playWhenReady,
                            )
                        }
                    }

                    var controlTimeoutVisibility by remember {
                        mutableIntStateOf(
                            PLAYER_CONTROL_VISIBILITY_TIMEOUT
                        )
                    }
                    var areControlsVisible by remember { mutableStateOf(true) }
                    var areControlsLocked by remember { mutableStateOf(false) }
                    val snackbarBottomPadding by animateDpAsState(
                        targetValue = if (areControlsVisible) 100.dp else 0.dp,
                        label = ""
                    )

                    val isLastEpisode = remember(currentSelectedEpisode) {
                        val lastSeason = watchHistoryItem.seasons
                        val lastEpisode = watchHistoryItem.episodes[lastSeason]

                        currentSelectedEpisode?.season == lastSeason && currentSelectedEpisode?.episode == lastEpisode
                    }

                    fun showControls(isShowing: Boolean) {
                        controlTimeoutVisibility =
                            if (isShowing) PLAYER_CONTROL_VISIBILITY_TIMEOUT else 0
                    }

                    LaunchedEffect(Unit) {
                        val userDefaultBrightnessLevel = Settings.System.getInt(
                            contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            -1
                        ) / 255F
                        viewModel.updateScreenBrightness(userDefaultBrightnessLevel)
                    }

                    LaunchedEffect(controlTimeoutVisibility) {
                        if (controlTimeoutVisibility > 0) {
                            areControlsVisible = true
                            delay(1000)
                            controlTimeoutVisibility -= 1
                        } else areControlsVisible = false
                    }

                    InitializeAudioFocusChangeListener(
                        play = {
                            mediaSession?.player?.run {
                                play()
                                viewModel.updateIsPlayingState(isPlaying)
                            }
                        },
                        pause = {
                            mediaSession?.player?.run {
                                pause()
                                viewModel.updateIsPlayingState(isPlaying)
                            }
                        }
                    )

                    RequestAudioFocus(
                        isPlaying = uiState.isPlaying,
                        playbackState = uiState.playbackState,
                        onRequestGranted = {
                            if (playerTimeUpdaterJob?.isActive == true)
                                return@RequestAudioFocus

                            playerTimeUpdaterJob = scope.launch {
                                viewModel.updateIsPlayingState(true)

                                while (mediaSession?.player?.isPlaying == true) {
                                    viewModel.updateCurrentTime(mediaSession?.player?.currentPosition)
                                    delay(1.seconds / 30)
                                }
                            }
                        }
                    )

                    LaunchedEffect(
                        uiState.currentTime,
                        uiState.isPlaying,
                        uiState.totalDuration
                    ) {
                        val areThereLessThan10SecondsLeft = areThereLessThan10SecondsLeftToWatch(
                            uiState.currentTime,
                            uiState.totalDuration
                        )
                        val isPlayerInitialized = uiState.isPlaying && uiState.totalDuration > 0L
                        val isTvShow = seasonCount != null

                        val isInPipMode =
                            if (SDK_INT >= Build.VERSION_CODES.N)
                                isInPictureInPictureMode
                            else false

                        if (isPlayerInitialized && areThereLessThan10SecondsLeft && !isLastEpisode && isTvShow && !isInPipMode) {
                            val secondsLeft = (uiState.totalDuration - uiState.currentTime) / 1000

                            if (secondsLeft <= 0L) {
                                viewModel.showSnackbar(
                                    message = "Loading next episode...",
                                    type = PlayerSnackbarMessageType.Episode
                                )
                                return@LaunchedEffect
                            }

                            viewModel.showSnackbar(
                                message = "Next episode on $secondsLeft...",
                                type = PlayerSnackbarMessageType.Episode
                            )
                        }
                    }

                    CompositionLocalProvider(LocalPlayer provides mediaSession?.player) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showControls(!areControlsVisible)
                                }
                        ) {
                            LifecycleAwarePlayer(
                                isInPipModeProvider = {
                                    if (SDK_INT >= Build.VERSION_CODES.N) {
                                        isInPictureInPictureMode
                                    } else false
                                },
                                appSettings = appSettings,
                                areControlsVisible = areControlsVisible && !areControlsLocked,
                                onEventCallback = { duration, currentPosition, bufferPercentage, isPlaying, playbackState ->
                                    viewModel.updatePlayerState(
                                        totalDuration = duration,
                                        currentTime = currentPosition,
                                        bufferedPercentage = bufferPercentage,
                                        isPlaying = isPlaying,
                                        playbackState = playbackState
                                    )
                                },
                                playWhenReady = uiState.playWhenReady,
                                onPlaybackReady = {
                                    mediaSession?.player?.run {
                                        // Initialize player states
                                        viewModel.updateIsPlayingState(playWhenReady)
                                        playbackParameters =
                                            PlaybackParameters(uiState.playbackSpeed)

                                        viewModel.initializeVideoQualities(currentTracks)
                                        viewModel.updateWatchHistory()

                                        trackSelectionParameters = viewModel.onSubtitleChange(
                                            subtitleIndex = uiState.selectedSubtitle,
                                            trackParameters = trackSelectionParameters
                                        )
                                        trackSelectionParameters = viewModel.onVideoQualityChange(
                                            qualityIndex = uiState.selectedQuality,
                                            trackParameters = trackSelectionParameters
                                        ) ?: return@run
                                    }
                                },
                                onInitialize = {
                                    if (mediaSession != null) {
                                        mediaSession!!.player.addListener(it)
                                        return@LifecycleAwarePlayer
                                    }

                                    mediaSession = initializePlayer(
                                        currentSource,
                                        videoData.title,
                                        subtitlesList,
                                        uiState.currentTime,
                                        uiState.playWhenReady
                                    )
                                    mediaSession!!.player.addListener(it)
                                },
                                onRelease = {
                                    mediaSession?.run {
                                        viewModel.onActivityStop(
                                            playWhenReady = player.isPlaying && player.playWhenReady,
                                            currentTime = player.currentPosition
                                        )
                                        viewModel.updateWatchHistory()

                                        player.removeListener(it)
                                        player.release()
                                        release()
                                        mediaSession = null
                                    }
                                },
                                onPlaybackEnded = {
                                    val isInPipMode = if (SDK_INT >= Build.VERSION_CODES.N) {
                                        isInPictureInPictureMode
                                    } else false

                                    if (!isLastEpisode && !isInPipMode)
                                        viewModel.onEpisodeClick()
                                }
                            )

                            PlayerControls(
                                visibilityProvider = {
                                    areControlsVisible &&
                                            !(if (SDK_INT >= Build.VERSION_CODES.N) isInPictureInPictureMode else false)
                                },
                                areControlsLocked = areControlsLocked,
                                watchHistoryItem = watchHistoryItem,
                                videoData = videoData,
                                availableSeasons = seasonCount,
                                currentEpisodeSelected = currentSelectedEpisode,
                                isLastEpisode = isLastEpisode,
                                videoQualities = availableQualities,
                                stateProvider = { uiState },
                                seasonDataProvider = { seasonData },
                                onBack = ::finish,
                                showControls = { showControls(it) },
                                toggleControlLock = { areControlsLocked = it },
                                onBrightnessChange = {
                                    setBrightness(it)
                                    viewModel.updateScreenBrightness(it)
                                    showControls(true)
                                },
                                onPauseToggle = {
                                    viewModel.updateIsPlayingState()
                                    shouldPlay = uiState.isPlaying
                                },
                                onSnackbarToggle = viewModel::showSnackbar,
                                onSeasonChange = viewModel::onSeasonChange,
                                onSubtitleChange = { subtitleIndex, trackSelectionParameters ->
                                    viewModel.onSubtitleChange(
                                        subtitleIndex = subtitleIndex,
                                        trackParameters = trackSelectionParameters
                                    )
                                },
                                onVideoQualityChange = { videoQualityIndex, trackSelectionParameters ->
                                    viewModel.onVideoQualityChange(
                                        qualityIndex = videoQualityIndex,
                                        trackParameters = trackSelectionParameters
                                    )
                                },
                                onVideoServerChange = {
                                    viewModel.onVideoServerChange(serverIndex = it)
                                },
                                onPlaybackSpeedChange = {
                                    viewModel.onPlaybackSpeedChange(speedIndex = it)
                                },
                                onEpisodeClick = viewModel::onEpisodeClick
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .widthIn(min = 250.dp, max = 465.dp)
                                    .heightIn(min = 65.dp, max = 195.dp)
                                    .padding(bottom = snackbarBottomPadding)
                            ) {
                                LazyColumn {
                                    itemsIndexed(viewModel.snackbarQueue) { i, data ->
                                        PlayerSnackbarVisuals(
                                            messageData = data,
                                            onDismissMessage = {
                                                viewModel.removeSnackbar(i)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (dialogState !is VideoDataDialogState.Idle) {
                        if (mediaSession?.player?.isPlaying == true) {
                            mediaSession?.player?.pause()
                        }

                        VideoPlayerDialog(
                            videoDataDialogState = dialogState,
                            onConsumeDialog = {
                                // If something error has happened,
                                // we will go back to the tv show details screen
                                if (dialogState is VideoDataDialogState.Error || dialogState is VideoDataDialogState.Unavailable) {
                                    this@PlayerActivity.finish()
                                }

                                viewModel.onConsumePlayerDialog()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.run {
            viewModel.updateWatchHistory()

            val videoData = getSerializableExtra(VIDEO_DATA)
            val watchHistoryItem = getSerializableExtra(WATCH_HISTORY_ITEM) ?: WatchHistoryItem()
            val episodeSelected = getSerializableExtra(EPISODE_SELECTED)
            val seasonCount = getIntExtra(SEASON_COUNT, 1)

            viewModel.savedStateHandle[VIDEO_DATA] = videoData
            viewModel.savedStateHandle[WATCH_HISTORY_ITEM] = watchHistoryItem

            viewModel.savedStateHandle[EPISODE_SELECTED] = episodeSelected
            viewModel.savedStateHandle[SEASON_COUNT] = if (episodeSelected != null)
                seasonCount
            else null
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                with(PictureInPictureParams.Builder()) {
                    val width = 16
                    val height = 9
                    setAspectRatio(Rational(width, height))
                    build()
                }
            )
        }
    }

    @Composable
    private fun InitializeAudioFocusChangeListener(
        play: () -> Unit,
        pause: () -> Unit,
    ) {
        LaunchedEffect(Unit) {
            afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (playbackDelayed || resumeOnFocusGain) {
                            synchronized(focusLock) {
                                playbackDelayed = false
                                resumeOnFocusGain = false
                            }
                            play()
                        }
                    }

                    AudioManager.AUDIOFOCUS_LOSS -> {
                        synchronized(focusLock) {
                            resumeOnFocusGain = false
                            playbackDelayed = false
                        }
                        pause()
                    }

                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        synchronized(focusLock) {
                            // only resume if playback is being interrupted
                            resumeOnFocusGain = shouldPlay
                            playbackDelayed = false
                        }
                        pause()
                    }

                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        synchronized(focusLock) {
                            // only resume if playback is being interrupted
                            resumeOnFocusGain = shouldPlay
                            playbackDelayed = false
                        }
                        pause()
                    }
                }
            }
        }
    }

    @Composable
    private fun RequestAudioFocus(
        isPlaying: Boolean,
        playbackState: Int,
        onRequestGranted: () -> Unit,
    ) {
        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                val result = if (SDK_INT >= Build.VERSION_CODES.O) {
                    val playbackAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()

                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(afChangeListener)
                        .build()

                    audioManager.requestAudioFocus(focusRequest)
                } else {
                    audioManager.requestAudioFocus(
                        afChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN
                    )
                }

                synchronized(focusLock) {
                    playbackNowAuthorized = when (result) {
                        AudioManager.AUDIOFOCUS_REQUEST_FAILED -> false
                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                            onRequestGranted()
                            shouldPlay = true
                            true
                        }

                        AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                            playbackDelayed = true
                            false
                        }

                        else -> false
                    }
                }
            }
        }

        LaunchedEffect(isPlaying, playbackState) {
            if (isPlaying || playbackState == STATE_BUFFERING) {
                window?.addFlags(keepScreenOnFlag)
            } else {
                window?.clearFlags(keepScreenOnFlag)
            }
        }
    }

    private fun setBrightness(strength: Float) {
        window?.apply {
            attributes = attributes?.apply {
                screenBrightness = strength
            }
        }
    }
}