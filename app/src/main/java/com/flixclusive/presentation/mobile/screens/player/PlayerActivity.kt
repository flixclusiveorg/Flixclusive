package com.flixclusive.presentation.mobile.screens.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.utils.WatchHistoryUtils.calculateRemainingAmount
import com.flixclusive.domain.utils.WatchHistoryUtils.isTimeInRangeOfThreshold
import com.flixclusive.presentation.common.composables.SourceStateDialog
import com.flixclusive.presentation.common.player.FlixclusivePlayer
import com.flixclusive.presentation.common.player.PLAYER_CONTROL_VISIBILITY_TIMEOUT
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.LifecycleAwarePlayer
import com.flixclusive.presentation.common.player.utils.PlayerComposeUtils.LocalPlayer
import com.flixclusive.presentation.mobile.screens.player.controls.PlayerControls
import com.flixclusive.presentation.mobile.screens.player.utils.PlayerPiPUtils.updatePiPParams
import com.flixclusive.presentation.mobile.screens.player.utils.PlayerPipReceiver
import com.flixclusive.presentation.mobile.theme.FlixclusiveMobileTheme
import com.flixclusive.presentation.utils.ModifierUtils.noIndicationClickable
import com.flixclusive.providers.models.common.VideoData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

const val ACTION_PIP_CONTROL = "player_pip_control"
const val PLAYER_PIP_EVENT = "player_pip_event"

const val DEFAULT_PLAYER_SEEK_AMOUNT = 10000L
const val WATCH_HISTORY_ITEM = "watch_history_item"
const val SEASON_COUNT = "season_count"
const val EPISODE_SELECTED = "episode_selected"
const val VIDEO_DATA = "video_data"

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
    private val flixclusivePlayer by lazy {
        FlixclusivePlayer(
            context = this,
            appSettings = viewModel.appSettings.value
        )
    }

    private val maxVolume: Int
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    private val currentVolume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlixclusiveMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                    var currentSubtitlesSize by remember { mutableIntStateOf(videoData.subtitles.size) }

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

                        val isLoading = (!flixclusivePlayer.hasBeenInitialized
                                || !flixclusivePlayer.isPlaying
                                || flixclusivePlayer.playbackState == STATE_BUFFERING
                                || flixclusivePlayer.playbackState == STATE_ENDED) && !viewModel.areControlsLocked

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

                                flixclusivePlayer.run {
                                    handleBroadcastEvents(event)

                                    if (SDK_INT >= Build.VERSION_CODES.O) {
                                        updatePiPParams(
                                            isPlaying = isPlaying,
                                            hasEnded = playbackState == STATE_ENDED,
                                            preferredSeekIncrement = appSettings.preferredSeekAmount,
                                        )
                                    }
                                }
                            }
                        }
                    )

                    LaunchedEffect(flixclusivePlayer.playbackState) {
                        when (flixclusivePlayer.playbackState) {
                            STATE_READY -> {
                                // Find preferred audio language
                                flixclusivePlayer.onAudioChange(language = watchHistoryItem.film.language)
                            }

                            STATE_ENDED -> {
                                val isInPipMode = if (SDK_INT >= Build.VERSION_CODES.N) {
                                    isInPictureInPictureMode
                                } else false

                                if (!viewModel.isLastEpisode && !isInPipMode) {
                                    flixclusivePlayer.run {
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

                    LaunchedEffect(Unit) {
                        // Initialize brightness
                        val userDefaultBrightnessLevel = Settings.System.getInt(
                            contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            -1
                        ) / 255F

                        viewModel.updateScreenBrightness(userDefaultBrightnessLevel)

                        // Initialize volume level
                        viewModel.updateVolume(percentOfVolume())
                    }

                    LaunchedEffect(controlTimeoutVisibility) {
                        if (controlTimeoutVisibility > 0) {
                            viewModel.areControlsVisible = true
                            delay(1000)
                            controlTimeoutVisibility -= 1
                        } else viewModel.areControlsVisible = false
                    }

                    // Re-prepare the player if video data has changed
                    LaunchedEffect(
                        videoData.source,
                        flixclusivePlayer.availableSubtitles.size
                    ) {
                        if (flixclusivePlayer.availableSubtitles.size < videoData.subtitles.size) {
                            return@LaunchedEffect
                        }

                        val currentAutoAdaptiveSource = videoData.source
                        val isNewServer =
                            !currentAutoAdaptiveSource.equals(
                                currentSource,
                                ignoreCase = true
                            ) && videoData.mediaId == currentMediaId

                        val hasNewSubtitle = currentSubtitlesSize < videoData.subtitles.size
                        val isNewData = videoData.mediaId != currentMediaId

                        if (isNewData) {
                            async { viewModel.resetUiState() }.await()
                            currentMediaId = videoData.mediaId
                        }

                        if (isNewServer || isNewData || hasNewSubtitle) {
                            currentSubtitlesSize = videoData.subtitles.size
                            currentSource = videoData.source

                            val (currentPosition, _) = viewModel.getSavedTimeForVideoData(
                                currentSelectedEpisode
                            )

                            flixclusivePlayer.prepare(
                                videoData = videoData,
                                initialPlaybackPosition = currentPosition
                            )
                        }
                    }

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
                        flixclusivePlayer.hasBeenInitialized,
                        viewModel.areControlsLocked,
                        flixclusivePlayer.isPlaying,
                        flixclusivePlayer.playbackState,
                        isEpisodesSheetOpened.value,
                        isPlayerSettingsDialogOpened.value,
                        isAudiosAndSubtitlesDialogOpened.value,
                        isServersDialogOpened.value,
                    ) {
                        showControls(true)
                    }

                    InitializeAudioFocusChangeListener(
                        play = {
                            flixclusivePlayer.play()
                            flixclusivePlayer.playWhenReady = true
                        },
                        pause = {
                            flixclusivePlayer.pause()
                            flixclusivePlayer.playWhenReady = false
                        }
                    )

                    RequestAudioFocus(
                        isPlaying = flixclusivePlayer.isPlaying,
                        playbackState = flixclusivePlayer.playbackState,
                        onRequestGranted = {
                            if (playerTimeUpdaterJob?.isActive == true)
                                return@RequestAudioFocus

                            playerTimeUpdaterJob = scope.launch {
                                if (SDK_INT >= Build.VERSION_CODES.O) {
                                    updatePiPParams(
                                        isPlaying = true,
                                        hasEnded = false,
                                        preferredSeekIncrement = appSettings.preferredSeekAmount,
                                    )
                                }

                                flixclusivePlayer.observePlayerPosition()
                            }
                        }
                    )

                    LaunchedEffect(
                        flixclusivePlayer.currentPosition,
                        flixclusivePlayer.isPlaying,
                        flixclusivePlayer.duration,
                        viewModel.isLastEpisode
                    ) {
                        flixclusivePlayer.run {
                            val areThereLessThan10SecondsLeft = isTimeInRangeOfThreshold(
                                currentWatchTime = currentPosition,
                                totalDurationToWatch = duration,
                            )

                            val areThere20PercentLeft = isTimeInRangeOfThreshold(
                                currentWatchTime = currentPosition,
                                totalDurationToWatch = duration,
                                threshold = calculateRemainingAmount(
                                    amount = duration,
                                    percentage = 0.8
                                )
                            )
                            val isPlayerInitialized = isPlaying && duration > 0L
                            val isTvShow = seasonCount != null

                            val isInPipMode =
                                if (SDK_INT >= Build.VERSION_CODES.N)
                                    isInPictureInPictureMode
                                else false

                            if (
                                isPlayerInitialized
                                && !viewModel.isLastEpisode
                                && isTvShow
                                && !isInPipMode
                            ) {
                                if (
                                    areThere20PercentLeft
                                    && !areThereLessThan10SecondsLeft
                                ) {
                                    viewModel.onQueueNextEpisode()
                                }

                                if (areThereLessThan10SecondsLeft) {
                                    val secondsLeft = (duration - currentPosition) / 1000

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
                        }
                    }

                    CompositionLocalProvider(LocalPlayer provides flixclusivePlayer) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            LifecycleAwarePlayer(
                                isInPipModeProvider = {
                                    if (SDK_INT >= Build.VERSION_CODES.N) {
                                        isInPictureInPictureMode
                                    } else false
                                },
                                areControlsVisible = viewModel.areControlsVisible && !viewModel.areControlsLocked,
                                resizeMode = uiState.selectedResizeMode,
                                releaseOnStop = appSettings.shouldReleasePlayer,
                                onInitialize = {
                                    flixclusivePlayer.run {
                                        val (currentPosition, _) = viewModel.getSavedTimeForVideoData(
                                            currentSelectedEpisode
                                        )

                                        initialize()
                                        prepare(
                                            videoData = videoData,
                                            initialPlaybackPosition = currentPosition
                                        )
                                    }
                                },
                                onRelease = {
                                    flixclusivePlayer.run {
                                        viewModel.updateWatchHistory(
                                            currentTime = currentPosition,
                                            duration = duration
                                        )

                                        release()
                                    }
                                },
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
                                            !(if (SDK_INT >= Build.VERSION_CODES.N) isInPictureInPictureMode else false)
                                },
                                appSettings = appSettings,
                                areControlsLocked = viewModel.areControlsLocked,
                                isEpisodesSheetOpened = isEpisodesSheetOpened,
                                isAudiosAndSubtitlesDialogOpened = isAudiosAndSubtitlesDialogOpened,
                                isPlayerSettingsDialogOpened = isPlayerSettingsDialogOpened,
                                isServersDialogOpened = isServersDialogOpened,
                                watchHistoryItem = watchHistoryItem,
                                videoData = videoData,
                                sourceProviders = viewModel.sourceProviders,
                                availableSeasons = seasonCount,
                                currentEpisodeSelected = currentSelectedEpisode,
                                isLastEpisode = viewModel.isLastEpisode,
                                stateProvider = { uiState },
                                seasonDataProvider = { seasonData },
                                onBack = ::finish,
                                showControls = { showControls(it) },
                                toggleControlLock = { viewModel.areControlsLocked = it },
                                onBrightnessChange = {
                                    setBrightness(it)
                                    viewModel.updateScreenBrightness(it)
                                },
                                onVolumeChange = {
                                    flixclusivePlayer.run {
                                        val newVolume = (it * 15).roundToInt()

                                        setDeviceVolume(newVolume)
                                        viewModel.updateVolume(it)
                                    }
                                },
                                onSnackbarToggle = viewModel::showSnackbar,
                                onSeasonChange = viewModel::onSeasonChange,
                                onVideoServerChange = viewModel::onServerChange,
                                onSourceChange = viewModel::changeSource,
                                onResizeModeChange = viewModel::onResizeModeChange,
                                onPanelChange = viewModel::onPanelChange,
                                onEpisodeClick = {
                                    flixclusivePlayer.run {
                                        viewModel.updateWatchHistory(
                                            currentTime = currentPosition,
                                            duration = duration
                                        )
                                    }
                                    onEpisodeClick(it)
                                },
                                toggleVideoTimeReverse = viewModel::toggleVideoTimeReverse,
                            )

                            if (
                                if (SDK_INT >= Build.VERSION_CODES.N) {
                                    !isInPictureInPictureMode
                                } else true
                            ) {
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

                    if (dialogState !is VideoDataDialogState.Idle) {
                        LaunchedEffect(Unit) {
                            flixclusivePlayer.run {
                                if (isPlaying) {
                                    pause()
                                    playWhenReady = true
                                }
                            }
                        }

                        SourceStateDialog(
                            state = dialogState,
                            onConsumeDialog = {
                                viewModel.onConsumePlayerDialog()

                                if (flixclusivePlayer.playWhenReady) {
                                    flixclusivePlayer.play()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun percentOfVolume(volume: Int? = null): Float {
        return ((volume ?: currentVolume) / maxVolume.toFloat()).coerceIn(0F, 1F)
    }

    @Suppress("DEPRECATION")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.run {
            viewModel.updateWatchHistory(
                currentTime = flixclusivePlayer.currentPosition,
                duration = flixclusivePlayer.duration
            )

            val videoData = getSerializableExtra(VIDEO_DATA)
            val watchHistoryItem =
                getSerializableExtra(WATCH_HISTORY_ITEM) ?: WatchHistoryItem()
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
            val preferredSeekIncrement = viewModel.appSettings.value.preferredSeekAmount

            enterPictureInPictureMode(
                updatePiPParams(
                    isPlaying = flixclusivePlayer.isPlaying,
                    hasEnded = flixclusivePlayer.playbackState == STATE_ENDED,
                    preferredSeekIncrement = preferredSeekIncrement
                )
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        /*
        *
        * Hide the system bars for immersive experience
        * */
        if (hasFocus && SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.run {
                systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.ime())
                hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (event.keyCode) {
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

            else -> super.onKeyDown(keyCode, event)
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
                    @Suppress("DEPRECATION")
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
                            flixclusivePlayer.playWhenReady = true
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