package com.flixclusive.core.ui.player

import android.content.Context
import android.graphics.Color
import android.media.audiofx.LoudnessEnhancer
import android.util.TypedValue
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEachIndexed
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer.STATE_ENABLED
import androidx.media3.exoplayer.Renderer.STATE_STARTED
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.awaitFirst
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.player.renderer.CustomDecoder
import com.flixclusive.core.ui.player.util.MimeTypeParser
import com.flixclusive.core.ui.player.util.PlayerCacheManager
import com.flixclusive.core.ui.player.util.PlayerUiUtil.availablePlaybackSpeeds
import com.flixclusive.core.ui.player.util.VolumeManager
import com.flixclusive.core.ui.player.util.disableSSLVerification
import com.flixclusive.core.ui.player.util.extensions.getFormats
import com.flixclusive.core.ui.player.util.extensions.getIndexOfPreferredLanguage
import com.flixclusive.core.ui.player.util.extensions.getName
import com.flixclusive.core.ui.player.util.extensions.getSubtitleSource
import com.flixclusive.core.ui.player.util.extensions.handleError
import com.flixclusive.core.ui.player.util.extensions.switchTrack
import com.flixclusive.core.ui.player.util.extensions.toSubtitleConfigurations
import com.flixclusive.core.ui.player.util.getCacheFactory
import com.flixclusive.core.ui.player.util.getLoadControl
import com.flixclusive.core.ui.player.util.getRenderers
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.okhttp.USER_AGENT
import com.flixclusive.model.datastore.user.PlayerPreferences
import com.flixclusive.model.datastore.user.SubtitlesPreferences
import com.flixclusive.model.datastore.user.UserPreferences
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.Locale
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds
import com.flixclusive.core.locale.R as LocaleR

/** toleranceBeforeUs – The maximum time that the actual position seeked to may precede the
 * requested seek position, in microseconds. Must be non-negative. */
private const val TOLERANCE_BEFORE_US = 300_000L

/**
 * toleranceAfterUs – The maximum time that the actual position seeked to may exceed the requested
 * seek position, in microseconds. Must be non-negative.
 */
private const val TOLERANCE_AFTER_US = 300_000L

private const val FONT_SIZE_PIP_MODE = 8F // Equivalent to 8dp

const val PLAYER_CONTROL_VISIBILITY_TIMEOUT = 5

enum class PlayerEvents {
    PLAY,
    PAUSE,
    REPLAY,
    FORWARD,
    BACKWARD,
}

/**
 *
 * A Player wrapper/manager for [Player], [ExoPlayer] and [MediaSession]
 * TODO: Improve code.
 */
@OptIn(UnstableApi::class)
class FlixclusivePlayerManager(
    client: OkHttpClient,
    private val dataStoreManager: DataStoreManager,
    private val context: Context,
    private val playerCacheManager: PlayerCacheManager,
    private val showErrorCallback: (message: UiText) -> Unit,
) : Player.Listener {
    private val playerPreferences
        get() =
            dataStoreManager
                .getUserPrefs<PlayerPreferences>(UserPreferences.PLAYER_PREFS_KEY)
                .awaitFirst()

    private val subtitlesPreferences
        get() =
            dataStoreManager
                .getUserPrefs<SubtitlesPreferences>(UserPreferences.SUBTITLES_PREFS_KEY)
                .awaitFirst()

    private var mediaSession: MediaSession? = null
    private var currentStreamPlaying: Stream? = null
    private var areTracksInitialized: Boolean = false

    var player: ExoPlayer? by mutableStateOf(null)
        private set
    var hasBeenInitialized by mutableStateOf(false)
        private set

    var duration by mutableLongStateOf(0L)
        private set
    var currentPosition by mutableLongStateOf(0L)
        private set
    var bufferedPercentage by mutableIntStateOf(0)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var playbackState by mutableIntStateOf(Player.STATE_IDLE)
        private set

    var playbackSpeed by mutableFloatStateOf(1F)
        private set
    var selectedAudioIndex by mutableIntStateOf(0)
        private set
    var selectedSubtitleIndex by mutableIntStateOf(0)
        private set
    var subtitleOffset by mutableLongStateOf(0L)
        private set
    var playWhenReady by mutableStateOf(true)

    private var preferredAudioLanguage = playerPreferences.audioLanguage
    var preferredSubtitleLanguage = subtitlesPreferences.subtitleLanguage

    val displayTitle: String
        get() = (player?.mediaMetadata?.displayTitle ?: "").toString()

    val volumeManager = VolumeManager(context)

    private lateinit var cacheFactory: DefaultMediaSourceFactory
    private val localDataSource: DefaultDataSource.Factory
    private val okHttpDataSource: OkHttpDataSource.Factory
    private val baseHttpDataSource =
        DefaultHttpDataSource
            .Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)

    // private var currentSubtitleDecoder: CustomSubtitleDecoderFactory? = null
    private var currentTextRenderer: TextRenderer? = null

    // == CCs/Audios/Qualities
    private val audioTrackGroups: MutableList<Tracks.Group?> = mutableListOf()
    val availableAudios = mutableStateListOf<String>()
    val availableSubtitles = mutableStateListOf<Subtitle>()
    // ==

    init {
        localDataSource = DefaultDataSource.Factory(context, baseHttpDataSource)
        okHttpDataSource =
            OkHttpDataSource
                .Factory(client)
                .setUserAgent(USER_AGENT)
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        duration = player.duration.coerceAtLeast(0L)
        currentPosition = player.currentPosition.coerceIn(0L, duration)
        playbackState = player.playbackState
        isPlaying = player.isPlaying
        bufferedPercentage = player.bufferedPercentage
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> onReady()
            Player.STATE_ENDED -> onSubtitleOffsetChange(0)
            else -> Unit
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        errorLog(error)
        error.handleError(
            duration = player?.duration,
            showErrorCallback = showErrorCallback,
        ) {
            player?.run {
                seekToDefaultPosition()
                prepare()
                playWhenReady = this@FlixclusivePlayerManager.playWhenReady
            }
        }
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        super.onAudioSessionIdChanged(audioSessionId)
        volumeManager.loudnessEnhancer?.release()

        safeCall {
            volumeManager.loudnessEnhancer = LoudnessEnhancer(audioSessionId)
        }
    }

    fun initialize() {
        if (player == null) {
            infoLog("Initializing the player...")
            disableSSLVerification()

            val trackSelector = DefaultTrackSelector(context)
            val loadControl =
                getLoadControl(
                    bufferCacheSize = playerPreferences.bufferCacheSize,
                    videoBufferMs = playerPreferences.videoBufferMs,
                )

            cacheFactory =
                context.getCacheFactory(
                    cache =
                        playerCacheManager.getCache(
                            preferredDiskCacheSize = playerPreferences.diskCacheSize,
                        ),
                    onlineDataSource = okHttpDataSource,
                )

            player =
                ExoPlayer
                    .Builder(context)
                    .apply {
                        setDeviceVolumeControlEnabled(true)
                        setTrackSelector(trackSelector)

                        setMediaSourceFactory(cacheFactory)

                        setRenderersFactory {
                            eventHandler,
                            videoRendererEventListener,
                            audioRendererEventListener,
                            textRendererOutput,
                            metadataRendererOutput,
                            ->
                            context.getRenderers(
                                eventHandler = eventHandler,
                                videoRendererEventListener = videoRendererEventListener,
                                audioRendererEventListener = audioRendererEventListener,
                                textRendererOutput = textRendererOutput,
                                metadataRendererOutput = metadataRendererOutput,
                                subtitleOffset = subtitleOffset,
                                decoderPriority = playerPreferences.decoderPriority,
                                onTextRendererChange = {
                                    currentTextRenderer = it
                                },
                            )
                        }
                        playerPreferences.seekAmount.let {
                            setSeekBackIncrementMs(it)
                            setSeekForwardIncrementMs(it)
                        }

                        setSeekParameters(SeekParameters(TOLERANCE_BEFORE_US, TOLERANCE_AFTER_US))
                        setLoadControl(loadControl)
                    }.build()
                    .apply {
                        setHandleAudioBecomingNoisy(true)
                        addListener(this@FlixclusivePlayerManager)
                    }
        }

        infoLog("Initializing the media session...")
        mediaSession =
            MediaSession
                .Builder(context, player!!)
                .build()

        if (playerPreferences.isUsingVolumeBoost) {
            infoLog("Initializing the volume booster...")
            safeCall {
                volumeManager.loudnessEnhancer = LoudnessEnhancer(player!!.audioSessionId)
            }
        }
    }

    fun prepare(
        link: Stream,
        title: String,
        subtitles: List<Subtitle>,
        initialPlaybackPosition: Long = 0L,
    ) {
        if (player == null) return

        infoLog("Preparing the player...")

        if (link != currentStreamPlaying) {
            currentStreamPlaying = link
            areTracksInitialized = false
        }

        val customHeaders = link.customHeaders ?: emptyMap()

        okHttpDataSource.setDefaultRequestProperties(customHeaders)
        baseHttpDataSource.setDefaultRequestProperties(customHeaders)


        val mediaItem = createMediaItem(url = link.url, title = title, subtitles = subtitles)
        val mediaSource = cacheFactory.createMediaSource(mediaItem)

        player!!.setMediaSource(
            mediaSource,
            initialPlaybackPosition,
        )

        player!!.prepare()
        player!!.playWhenReady = playWhenReady
    }

    fun release(isForceReleasing: Boolean = false) {
        val isFullyReleasingThePlayer = playerPreferences.isForcingPlayerRelease

        if (isFullyReleasingThePlayer || isForceReleasing) {
            infoLog("Releasing the player...")
            hasBeenInitialized = false
            playerCacheManager.releaseCache()
            player?.removeListener(this@FlixclusivePlayerManager)
            player?.release()

            player = null
            currentTextRenderer = null
        }

        infoLog("Releasing the media session...")
        mediaSession?.release()
        mediaSession = null
    }

    private fun onReady() {
        if (player == null) return

        if (!areTracksInitialized) {
            extractAudios()
            extractSubtitles()

            val subtitleIndex =
                if (!subtitlesPreferences.isSubtitleEnabled) 0
                else availableSubtitles.getIndexOfPreferredLanguage(
                    preferredLanguage = preferredSubtitleLanguage,
                    languageExtractor = { it.language },
                )

            val audioIndex =
                availableAudios.getIndexOfPreferredLanguage(
                    preferredLanguage = preferredAudioLanguage,
                    languageExtractor = { it },
                )

            onSubtitleChange(index = subtitleIndex)
            onAudioChange(index = audioIndex)
        } else {
            onSubtitleChange(index = selectedSubtitleIndex)
            onAudioChange(index = selectedAudioIndex)
        }

        player?.setPlaybackSpeed(playbackSpeed)

        areTracksInitialized = true
        hasBeenInitialized = true
    }

    private fun createMediaItem(
        url: String,
        title: String?,
        subtitles: List<Subtitle>,
    ): MediaItem {
        val metadata = MediaMetadata.Builder().setDisplayTitle(title).build()
        val subtitleConfigurations = subtitles.toSubtitleConfigurations(currentList = availableSubtitles)

        return MediaItem
            .Builder()
            .setUri(url)
            .setMediaId(url)
            .setSubtitleConfigurations(subtitleConfigurations)
            .setMediaMetadata(metadata).apply {
                if (MimeTypeParser.isM3U8(url)) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }.build()
    }

    /**
     * Extracts the embedded audios by populating
     * the available audios and track groups.
     *
     */
    private fun extractAudios() {
        val trackGroups = player?.currentTracks?.groups?.fastFilter {
            it.type == C.TRACK_TYPE_AUDIO && it.isSupported
        } ?: emptyList()

        audioTrackGroups.clear()
        availableAudios.clear()
        audioTrackGroups.addAll(trackGroups)

        trackGroups.getFormats().fastForEachIndexed { i, format ->
            val name = format.getName(C.TRACK_TYPE_AUDIO, i)
            availableAudios.add(name)
        }

        infoLog("Extracted ${availableAudios.size} audios!")
    }

    private fun extractSubtitles() {
        val subtitleTrackGroups =
            player?.currentTracks?.groups?.fastFilter { group ->
                group.type == C.TRACK_TYPE_TEXT && group.isSupported
            }

        availableSubtitles.clear()
        availableSubtitles.add(
            Subtitle(
                url = "",
                language = context.getString(LocaleR.string.off_subtitles),
                type = SubtitleSource.EMBEDDED,
            )
        )

        subtitleTrackGroups?.getFormats()?.fastForEachIndexed { i, format ->
            if (format.id == null) return@fastForEachIndexed

            availableSubtitles.add(
                element =
                    Subtitle(
                        url = format.id!!,
                        language = format.getName(C.TRACK_TYPE_TEXT, i),
                        type = getSubtitleSource(format.id!!),
                    ),
            )
        }

        infoLog("Extracted ${availableSubtitles.size - 1} subtitles!")
    }

    /**
     * Callback function triggered when the subtitles are changed.
     *
     * @param index The index of the selected subtitle.
     */
    fun onSubtitleChange(index: Int) {
        player?.apply {
            selectedSubtitleIndex = index
            preferredSubtitleLanguage = availableSubtitles.getOrNull(selectedSubtitleIndex)?.language
                ?: preferredSubtitleLanguage

            switchTrack(C.TRACK_TYPE_TEXT, selectedSubtitleIndex - 1)
        }
    }

    /**
     * Callback function triggered when the audio track is changed.
     *
     * @param index The index of the selected audio track.
     */
    fun onAudioChange(index: Int) {
        selectedAudioIndex = index
        val audioTrack =
            audioTrackGroups
                .getOrNull(selectedAudioIndex)
                ?.getTrackFormat(0)
                ?.getName(C.TRACK_TYPE_AUDIO, index)
                ?: return
        preferredAudioLanguage = audioTrack

        player?.switchTrack(C.TRACK_TYPE_AUDIO, index)
    }

    /**
     * Callback function triggered when the audio track is changed.
     * Though, this one needs the shorthand language name to change
     * the audio.
     *
     * @param language The language of the audio track
     */
    fun onAudioChange(language: String) {
        val index =
            availableAudios.indexOfFirst {
                it.contains(Locale(language).displayLanguage, ignoreCase = true)
            }

        onAudioChange(max(index, 0))
    }

    fun onPlaybackSpeedChange(speed: Int) {
        playbackSpeed = availablePlaybackSpeeds[speed]
        player?.setPlaybackSpeed(playbackSpeed)
    }

    fun onPlaybackSpeedChange(speed: Float) {
        playbackSpeed = speed
        player?.setPlaybackSpeed(playbackSpeed)
    }

    /**
     *
     * Adding delays on subtitle appearances
     * */
    fun onSubtitleOffsetChange(offset: Long) {
        subtitleOffset = offset
        CustomDecoder.subtitleOffset = offset
        if (currentTextRenderer?.state == STATE_ENABLED || currentTextRenderer?.state == STATE_STARTED) {
            currentTextRenderer?.resetPosition(currentPosition)
        }
    }

    /**
     *
     * For handling broadcast events sent
     * by the Picture-in-Picture mode
     * */
    fun handleBroadcastEvents(event: Int) {
        player?.run {
            when (event) {
                PlayerEvents.PLAY.ordinal -> {
                    play()
                    this@FlixclusivePlayerManager.playWhenReady = true
                }

                PlayerEvents.PAUSE.ordinal -> {
                    pause()
                    this@FlixclusivePlayerManager.playWhenReady = false
                }

                PlayerEvents.REPLAY.ordinal -> {
                    // Replay the video
                    seekTo(0)
                    this@FlixclusivePlayerManager.playWhenReady = true
                }

                PlayerEvents.FORWARD.ordinal -> seekForward()
                PlayerEvents.BACKWARD.ordinal -> seekBack()
            }
        }
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun seekBack() {
        player?.seekBack()
    }

    fun seekForward() {
        player?.seekForward()
    }

    /**
     *
     * Gets the current time of the player
     * in terms of where the position of the
     * player is.
     *
     * This will be used for the time bar/slider
     *
     */
    suspend fun observePlayerPosition() {
        player?.run {
            while (isPlaying) {
                this@FlixclusivePlayerManager.currentPosition = currentPosition
                delay(1.seconds / 30) // Important to add delay
            }
        }
    }

    /**
     *
     * Initializes the subtitle view's
     * subtitle style based on user's
     * preferences
     *
     * */
    fun setSubtitleStyle(
        subtitleView: SubtitleView?,
        areControlsVisible: Boolean,
        isInPictureInPictureMode: Boolean = false,
        isInTv: Boolean = false,
    ) {
        subtitleView?.run {
            // Add margin on subtitle view
            var subtitleMarginBottom = 0.05F

            if (areControlsVisible) {
                subtitleMarginBottom += 0.15F
            }

            setBottomPaddingFraction(subtitleMarginBottom)

            // Modify subtitle style
            val style =
                CaptionStyleCompat(
                    subtitlesPreferences.subtitleColor,
                    subtitlesPreferences.subtitleBackgroundColor,
                    Color.TRANSPARENT,
                    subtitlesPreferences.subtitleEdgeType.type,
                    Color.BLACK,
                    subtitlesPreferences.subtitleFontStyle.typeface,
                )

            val fontSize =
                if (isInPictureInPictureMode) {
                    FONT_SIZE_PIP_MODE
                } else {
                    subtitlesPreferences.subtitleSize
                }

            setApplyEmbeddedFontSizes(false)
            setApplyEmbeddedStyles(false)
            setStyle(style)
            setFixedTextSize(
                // unit =
                TypedValue.COMPLEX_UNIT_SP,
                // size =
                fontSize,
            )
        }
    }
}
