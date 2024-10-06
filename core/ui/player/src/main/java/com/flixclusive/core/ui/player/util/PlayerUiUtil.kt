package com.flixclusive.core.ui.player.util


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.player.FlixclusivePlayerManager
import com.flixclusive.core.ui.player.PlayerSnackbarMessageType
import com.flixclusive.core.ui.player.R
import com.flixclusive.core.ui.player.databinding.CustomPlayerBinding
import com.flixclusive.domain.provider.CachedLinks
import com.flixclusive.model.database.util.calculateRemainingTime
import com.flixclusive.model.database.util.isTimeInRangeOfThreshold
import com.flixclusive.model.datastore.player.ResizeMode
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.flixclusive.core.locale.R as LocaleR

@OptIn(UnstableApi::class)
object PlayerUiUtil {

    private const val FILM_TV_SHOW_TITLE_FORMAT = "S%d E%d: %s"

    val LocalPlayerManager = compositionLocalOf<FlixclusivePlayerManager?> { null }

    @Composable
    fun rememberLocalPlayerManager(): State<FlixclusivePlayerManager> {
        val player = LocalPlayerManager.current
        check(player != null) {
            stringResource(id = LocaleR.string.player_not_initialized)
        }

        return rememberUpdatedState(player)
    }

    fun formatPlayerTitle(
        film: Film,
        episode: Episode? = null
    ): String {
        if (episode == null && film.filmType == FilmType.TV_SHOW) {
            return film.title
        }

        return when(film.filmType) {
            FilmType.MOVIE -> film.title
            FilmType.TV_SHOW -> String.format(
                Locale.ROOT,
                FILM_TV_SHOW_TITLE_FORMAT,
                episode!!.season,
                episode.number,
                episode.title
            )
        }
    }

    /**
     *
     *
     * Formats milliseconds time to readable time string.
     * It also coerces the time on a minimum of 0.
     *
     * @param isInHours to check if the method would pad the string thrice.
     * */
    fun Long.formatMinSec(isInHours: Boolean = false): String {
        return if (this <= 0L && isInHours) {
            "00:00:00"
        } else if(this <= 0L) {
            "00:00"
        } else {
            val hours = TimeUnit.MILLISECONDS.toHours(this)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(this) -
                    TimeUnit.HOURS.toMinutes(hours)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(this) -
                    TimeUnit.MINUTES.toSeconds(minutes) -
                    TimeUnit.HOURS.toSeconds(hours)

            if (hours > 0 || isInHours) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("OpaqueUnitKey")
    @Composable
    fun LifecycleAwarePlayer(
        modifier: Modifier = Modifier,
        areControlsVisible: Boolean,
        isSubtitlesVisible: Boolean = true,
        isInPipMode: Boolean = false,
        isInTv: Boolean = false,
        resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
        onInitialize: () -> Unit,
        onRelease: (isForceReleasing: Boolean) -> Unit,
    ) {
        val lifecycle by rememberUpdatedState(LocalLifecycleOwner.current.lifecycle)
        val currentOnInitialize by rememberUpdatedState(onInitialize)
        val currentOnRelease by rememberUpdatedState(onRelease)
        val playerManager by rememberLocalPlayerManager()

        CustomPlayerView(
            modifier = modifier,
            areControlsVisible = areControlsVisible,
            isInTv = isInTv,
            isSubtitlesVisible = isSubtitlesVisible,
            isInPipMode = isInPipMode,
            resizeMode = resizeMode
        )

        DisposableEffect(LocalLifecycleOwner.current) {
            // Pre-initialize the player
            if (playerManager.player == null) {
                currentOnInitialize()
            }

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (playerManager.player == null) {
                            currentOnInitialize()
                        } else if (playerManager.playWhenReady) {
                            playerManager.play()
                        }
                    }

                    Lifecycle.Event.ON_STOP -> {
                        playerManager.playWhenReady = playerManager.isPlaying
                        playerManager.pause()

                        if (Build.VERSION.SDK_INT > 23) {
                            currentOnRelease(false)
                        }
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        if (Build.VERSION.SDK_INT <= 23) {
                            currentOnRelease(false)
                        }
                    }

                    else -> Unit
                }
            }

            lifecycle.addObserver(observer)

            onDispose {
                if(!isInPipMode) {
                    currentOnRelease(true)
                }
                lifecycle.removeObserver(observer)
            }
        }
    }

    @Composable
    private fun CustomPlayerView(
        modifier: Modifier = Modifier,
        areControlsVisible: Boolean,
        isSubtitlesVisible: Boolean,
        resizeMode: Int,
        isInTv: Boolean = false,
        isInPipMode: Boolean = false,
    ) {
        val playerManager by rememberLocalPlayerManager()

        AndroidViewBinding(
            modifier = modifier,
            factory = { inflater, viewGroup, attachToParent ->
                val binding = CustomPlayerBinding.inflate(inflater, viewGroup, attachToParent)

                binding.apply {
                    val subtitleViewHolder = root.findViewById<FrameLayout?>(R.id.subtitle_view_holder)

                    root.isClickable = false
                    root.isFocusable = false

                    playerView.run {
                        this@run.resizeMode = when (resizeMode) {
                            ResizeMode.Fill.ordinal -> ResizeMode.Fill.mode
                            ResizeMode.Zoom.ordinal -> ResizeMode.Zoom.mode
                            else -> ResizeMode.Fit.mode
                        }
                        this@run.player = playerManager.player

                        // Show the controls forever
                        controllerShowTimeoutMs = Int.MAX_VALUE
                        showController()

                        // Move out the SubtitleView ouf ot the content frame
                        // To avoid zooming in when resizeMode == CENTER_CROP
                        (subtitleView?.parent as ViewGroup?)?.removeView(subtitleView)
                        subtitleViewHolder?.addView(subtitleView)
                    }
                }

                return@AndroidViewBinding binding
            }
        ) {
            playerView.run {
                this.resizeMode = resizeMode
                this.player = playerManager.player

                if(!isControllerFullyVisible) {
                    showController()
                }

                subtitleView?.visibility = if (isSubtitlesVisible) View.VISIBLE else View.GONE

                if (isSubtitlesVisible) {
                    playerManager.setSubtitleStyle(
                        subtitleView = subtitleView,
                        isInPictureInPictureMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPipMode,
                        isInTv = isInTv,
                        areControlsVisible = areControlsVisible
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Composable
    fun AudioFocusManager(
        activity: Activity,
        preferredSeekAmount: Long,
        isPiPModeEnabled: Boolean = false,
        isTv: Boolean = false,
    ) {
        val playerManager by rememberLocalPlayerManager()
        val scope = rememberCoroutineScope()

        // Audio managers
        val audioManager = remember { activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        var afChangeListener: AudioManager.OnAudioFocusChangeListener? by remember { mutableStateOf(null) }
        var resumeOnFocusGain by rememberSaveable { mutableStateOf(false) }
        var playbackDelayed by rememberSaveable { mutableStateOf(false) }
        var playbackNowAuthorized by rememberSaveable { mutableStateOf(false) }
        var playerTimeUpdaterJob: Job? by remember { mutableStateOf(null) }
        val focusLock = remember { Any() }

        LaunchedEffect(Unit) {
            afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (playbackDelayed || resumeOnFocusGain) {
                            synchronized(focusLock) {
                                playbackDelayed = false
                                resumeOnFocusGain = false
                            }
                            playerManager.play()
                            playerManager.playWhenReady = true
                        }
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        synchronized(focusLock) {
                            // only resume if playback is being interrupted
                            resumeOnFocusGain = playerManager.playWhenReady
                            playbackDelayed = false
                        }
                        playerManager.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        synchronized(focusLock) {
                            resumeOnFocusGain = false
                            playbackDelayed = false
                        }
                        playerManager.pause()
                        playerManager.playWhenReady = false
                    }
                }
            }
        }

        LaunchedEffect(playerManager.isPlaying) {
            if(playerManager.isPlaying) {
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val playbackAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()

                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(afChangeListener!!)
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
                    playbackNowAuthorized = when(result) {
                        AudioManager.AUDIOFOCUS_REQUEST_FAILED -> false
                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                            if (playerTimeUpdaterJob?.isActive == true)
                                return@LaunchedEffect

                            playerTimeUpdaterJob = scope.launch {
                                playerManager.run {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isTv && isPiPModeEnabled) {
                                        activity.updatePiPParams(
                                            isPlaying = isPlaying,
                                            hasEnded = playbackState == Player.STATE_ENDED,
                                            preferredSeekIncrement = preferredSeekAmount,
                                        )
                                    }

                                    observePlayerPosition()
                                }
                            }
                            true
                        }
                        AudioManager.AUDIOFOCUS_REQUEST_DELAYED  -> {
                            playbackDelayed = true
                            false
                        }
                        else -> false
                    }
                }
            }
        }

        LaunchedEffect(playerManager.isPlaying, playerManager.playbackState) {
            val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

            if(playerManager.isPlaying || playerManager.playbackState == Player.STATE_BUFFERING) {
                activity.window?.addFlags(keepScreenOnFlag)
            } else {
                activity.window?.clearFlags(keepScreenOnFlag)
            }
        }
    }
    
    /**
     * 
     * Re-prepares the player if the provided [CachedLinks] has changed
     * 
     * */
    @Composable
    fun ObserveNewLinksAndSubtitles(
        selectedSourceLink: Int,
        currentPlayerTitle: String,
        newLinks: SnapshotStateList<Stream>,
        newSubtitles: SnapshotStateList<Subtitle>,
        getSavedTimeForCurrentSourceData: () -> Long
    ) {
        val playerManager by rememberLocalPlayerManager()
        var currentLoadedLink by remember { mutableStateOf(newLinks.getOrNull(selectedSourceLink)?.url) }
        var currentSubtitlesSize by remember { mutableIntStateOf(newSubtitles.size + 1) }

        val currentGetSavedTimeForCurrentSourceData by rememberUpdatedState(newValue = getSavedTimeForCurrentSourceData)

        LaunchedEffect(
            newLinks.getOrNull(0)?.url,
            newSubtitles.size,
            playerManager.hasBeenInitialized,
            selectedSourceLink
        ) {
            if (!playerManager.hasBeenInitialized) {
                return@LaunchedEffect
            }

            val newLink = newLinks.getOrNull(selectedSourceLink)
            val isNewServer =
                newLink?.url?.equals(
                    currentLoadedLink,
                    ignoreCase = true
                ) == false

            /* plus one because of the 'off' subtitle */
            val hasNewSubtitle = currentSubtitlesSize < (newSubtitles.size + 1)

            if ((isNewServer || hasNewSubtitle) && newLink != null) {
                currentLoadedLink = newLink.url
                currentSubtitlesSize = newSubtitles.size + 1

                // If user only has a new added subtitle
                // use the current position of the player
                val initialPlaybackPosition = if(!isNewServer) {
                    playerManager.currentPosition
                } else currentGetSavedTimeForCurrentSourceData()

                playerManager.prepare(
                    link = newLink,
                    title = currentPlayerTitle,
                    subtitles = newSubtitles.toList(),
                    initialPlaybackPosition = initialPlaybackPosition
                )
            }
        }
    }

    /**
     *
     * Observes the player current position/time.
     * This checks if the current player time is below
     * 10 seconds (if it is then show a loading next episode snackbar)
     * and 80% of the maximum time have been watched (if this is true
     * then proceed to queue up the next episode available).
     *
     * */
    @Composable
    fun ObservePlayerTime(
        isTvShow: Boolean,
        isLastEpisode: Boolean,
        isInPipMode: Boolean,
        showSnackbar: (message: UiText, type: PlayerSnackbarMessageType) -> Unit,
        onQueueNextEpisode: () -> Unit,
    ) {
        val playerManager by rememberLocalPlayerManager()
        
        LaunchedEffect(
            playerManager.currentPosition,
            playerManager.isPlaying,
            playerManager.duration,
            isLastEpisode,
            isInPipMode
        ) {
            playerManager.run {
                val areThereLessThan10SecondsLeft = isTimeInRangeOfThreshold(
                    currentWatchTime = currentPosition,
                    totalDurationToWatch = duration,
                )

                val areThere20PercentLeft = isTimeInRangeOfThreshold(
                    currentWatchTime = currentPosition,
                    totalDurationToWatch = duration,
                    threshold = calculateRemainingTime(
                        amount = duration,
                        percentage = 0.8
                    )
                )
                val isPlayerInitialized = isPlaying && duration > 0L

                if (
                    isPlayerInitialized
                    && !isLastEpisode
                    && isTvShow
                    && !isInPipMode
                ) {
                    if (
                        areThere20PercentLeft
                        && !areThereLessThan10SecondsLeft
                    ) {
                        onQueueNextEpisode()
                    }

                    if (areThereLessThan10SecondsLeft) {
                        val secondsLeft = (duration - currentPosition) / 1000

                        if (secondsLeft <= 0L) {
                            showSnackbar(
                                UiText.StringResource(LocaleR.string.loading_next_episode),
                                PlayerSnackbarMessageType.Episode
                            )
                            return@LaunchedEffect
                        }

                        showSnackbar(
                            UiText.StringResource(LocaleR.string.next_episode_on_format, secondsLeft),
                            PlayerSnackbarMessageType.Episode
                        )
                    }
                }
            }
        }
    }

    val availablePlaybackSpeeds = List(8) {
        0F + ((it + 1) * 0.25F)
    }
}