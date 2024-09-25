package com.flixclusive.core.ui.player

import android.content.Context
import android.graphics.Color
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.util.TypedValue
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastAny
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
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
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.player.renderer.CustomTextRenderer
import com.flixclusive.core.ui.player.util.MimeTypeParser
import com.flixclusive.core.ui.player.util.MimeTypeParser.toMimeType
import com.flixclusive.core.ui.player.util.PlayerCacheManager
import com.flixclusive.core.ui.player.util.PlayerTracksHelper.addOffSubtitle
import com.flixclusive.core.ui.player.util.PlayerTracksHelper.getIndexOfPreferredLanguage
import com.flixclusive.core.ui.player.util.PlayerUiUtil.availablePlaybackSpeeds
import com.flixclusive.core.ui.player.util.VolumeManager
import com.flixclusive.core.ui.player.util.disableSSLVerification
import com.flixclusive.core.ui.player.util.getCacheFactory
import com.flixclusive.core.ui.player.util.getLoadControl
import com.flixclusive.core.ui.player.util.getRenderers
import com.flixclusive.core.ui.player.util.handleError
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.network.okhttp.USER_AGENT
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.datastore.player.CaptionSizePreference.Companion.getDp
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.Locale
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

/** toleranceBeforeUs – The maximum time that the actual position seeked to may precede the
 * requested seek position, in microseconds. Must be non-negative. */
private const val toleranceBeforeUs = 300_000L

/**
 * toleranceAfterUs – The maximum time that the actual position seeked to may exceed the requested
 * seek position, in microseconds. Must be non-negative.
 */
private const val toleranceAfterUs = 300_000L

private const val FONT_SIZE_PIP_MODE = 8F // Equivalent to 8dp

const val PLAYER_CONTROL_VISIBILITY_TIMEOUT = 5

enum class PlayerEvents {
    PLAY,
    PAUSE,
    REPLAY,
    FORWARD,
    BACKWARD
}


/**
 *
 * A Player wrapper/manager for [Player], [ExoPlayer] and [MediaSession]
 *
 */
@OptIn(UnstableApi::class)
class FlixclusivePlayerManager(
    client: OkHttpClient,
    private var appSettings: AppSettings,
    private val context: Context,
    private val playerCacheManager: PlayerCacheManager,
    private val showErrorCallback: (message: UiText) -> Unit
) : Player.Listener {
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

    private var preferredAudioLanguage = appSettings.preferredAudioLanguage
    var preferredSubtitleLanguage = appSettings.subtitleLanguage

    val displayTitle: String
        get() = (player?.mediaMetadata?.displayTitle ?: "").toString()

    val volumeManager = VolumeManager(context)

    private lateinit var cacheFactory: DefaultMediaSourceFactory
    private val localDataSource: DefaultDataSource.Factory
    private val okHttpDataSource: OkHttpDataSource.Factory
    private var currentTextRenderer: CustomTextRenderer? = null
    private val baseHttpDataSource = DefaultHttpDataSource.Factory()
        .setUserAgent(USER_AGENT)
        .setAllowCrossProtocolRedirects(true)

    // == CCs/Audios/Qualities
    private val audioTrackGroups: MutableList<Tracks.Group?> = mutableListOf()
    val availableAudios = mutableStateListOf<String>()
    val availableSubtitles = mutableStateListOf<Subtitle>()
    // ==

    init {
        localDataSource = DefaultDataSource.Factory(context, baseHttpDataSource)
        okHttpDataSource = OkHttpDataSource.Factory(client)
            .setUserAgent(USER_AGENT)
    }

    override fun onEvents(player: Player, events: Player.Events) {
        duration = player.duration.coerceAtLeast(0L)
        currentPosition = player.currentPosition.coerceIn(0L, duration)
        playbackState = player.playbackState
        isPlaying = player.isPlaying
        bufferedPercentage = player.bufferedPercentage
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> onReady()
            else -> Unit
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        errorLog(error)
        error.handleError(
            duration = player?.duration,
            showErrorCallback = showErrorCallback
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
                    bufferCacheSize = appSettings.preferredBufferCacheSize,
                    videoBufferMs = appSettings.preferredVideoBufferMs
                )

            cacheFactory = context.getCacheFactory(
                cache = playerCacheManager.getCache(
                    preferredDiskCacheSize = appSettings.preferredDiskCacheSize
                ),
                onlineDataSource = okHttpDataSource
            )

            player = ExoPlayer.Builder(context)
                .apply {
                    setDeviceVolumeControlEnabled(true)
                    setTrackSelector(trackSelector)

                    setMediaSourceFactory(cacheFactory)

                    setRenderersFactory { eventHandler, videoRendererEventListener, audioRendererEventListener, textRendererOutput, metadataRendererOutput ->
                        context.getRenderers(
                            eventHandler = eventHandler,
                            videoRendererEventListener = videoRendererEventListener,
                            audioRendererEventListener = audioRendererEventListener,
                            textRendererOutput = textRendererOutput,
                            metadataRendererOutput = metadataRendererOutput,
                            subtitleOffset = subtitleOffset,
                            decoderPriority = appSettings.decoderPriority,
                            onTextRendererChange = {
                                currentTextRenderer = it
                            }
                        )
                    }
                    appSettings.preferredSeekAmount.let {
                        setSeekBackIncrementMs(it)
                        setSeekForwardIncrementMs(it)
                    }

                    setSeekParameters(SeekParameters(toleranceBeforeUs, toleranceAfterUs))
                    setLoadControl(loadControl)
                }
                .build()
                .apply {
                    setHandleAudioBecomingNoisy(true)
                    addListener(this@FlixclusivePlayerManager)
                }
        }

        infoLog("Initializing the media session...")
        mediaSession = MediaSession
            .Builder(context, player!!)
            .build()

        if (appSettings.isUsingVolumeBoost) {
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
        player?.run {
            infoLog("Preparing the player...")

            if (link != currentStreamPlaying) {
                currentStreamPlaying = link
                areTracksInitialized = false
            }

            val mediaItem = createMediaItem(
                url = link.url,
                title = title
            )

            val customHeaders = link.customHeaders ?: emptyMap()

            okHttpDataSource.setDefaultRequestProperties(customHeaders)
            baseHttpDataSource.setDefaultRequestProperties(customHeaders)

            val mediaSource = cacheFactory.createMediaSource(mediaItem)

            setMediaSource(
                /* mediaSource = */ MergingMediaSource(mediaSource, *createSubtitleSources(subtitles)),
                /* startPositionMs = */ initialPlaybackPosition
            )

            prepare()
            playWhenReady = this@FlixclusivePlayerManager.playWhenReady
        }
    }

    fun release(isForceReleasing: Boolean = false) {
        val isFullyReleasingThePlayer = appSettings.shouldReleasePlayer

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
        player?.run {
            if (!areTracksInitialized) {
                extractAudios()
                extractEmbeddedSubtitles()

                val subtitleIndex = when {
                    !appSettings.isSubtitleEnabled -> 0 // == Off subtitles
                    else -> availableSubtitles.getIndexOfPreferredLanguage(
                        preferredLanguage = preferredSubtitleLanguage,
                        languageExtractor = { it.language }
                    )
                }
                val audioIndex = availableAudios.getIndexOfPreferredLanguage(
                    preferredLanguage = preferredAudioLanguage,
                    languageExtractor = { it }
                )

                onSubtitleChange(index = subtitleIndex)
                onAudioChange(index = audioIndex)

                areTracksInitialized = true
            }

            setPlaybackSpeed(playbackSpeed)
            currentTextRenderer?.setRenderOffsetMs(offset = subtitleOffset)

            hasBeenInitialized = true
        }
    }

    private fun createMediaItem(
        url: String,
        title: String?
    ) = MediaItem.Builder()
        .setUri(url)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setDisplayTitle(title)
                .build()
        )
        .apply {
            if (MimeTypeParser.isM3U8(url)) {
                setMimeType(MimeTypes.APPLICATION_M3U8)
            }
        }
        .build()

    /**
     * Extracts the embedded audios by populating
     * the available audios and track groups.
     *
     */
    private fun extractAudios() {
        audioTrackGroups.clear()
        availableAudios.clear()

        player?.run {
            audioTrackGroups.addAll(
                currentTracks.groups
                    .filter { group ->
                        group.type == C.TRACK_TYPE_AUDIO
                    }
            )

            audioTrackGroups.forEach { group ->
                group?.let {
                    for (trackIndex in 0 until it.length) {
                        val format = it.getTrackFormat(trackIndex)
                        val locale = Locale(format.language ?: "Default")

                        availableAudios.add(
                            "Audio Track #${availableAudios.size + 1}: ${locale.displayLanguage}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Extracts the embedded subtitles by populating
     * the available audios and track groups.
     *
     */
    private fun extractEmbeddedSubtitles() {
        player?.run {
            val subtitleTrackGroups = currentTracks.groups
                .filter { group ->
                    group.type == C.TRACK_TYPE_TEXT
                }

            subtitleTrackGroups.getFormats().forEach { format ->
                // Filter out non subs, already used subs and subs without languages
                if (format.id == null || format.language == null)
                    return

                availableSubtitles.add(
                    element = Subtitle(
                        url = format.id ?: "",
                        language = format.language?.toUniqueSubtitleLanguage() ?: "Default Embedded",
                        type = SubtitleSource.EMBEDDED,
                    )
                )
            }
        }
    }

    /**
     * extracts the subtitle configurations of it.
     *
     */
    private fun createSubtitleSources(subtitles: List<Subtitle>): Array<SingleSampleMediaSource> {
        if (subtitles.isEmpty())
            return arrayOf()

        availableSubtitles.clear()

        val sortedSubtitles = subtitles
            .sortedWith(
                compareBy<Subtitle> { it.language.lowercase() }
                    .thenBy { it.language.first().isLetterOrDigit().not() }
            )
            .addOffSubtitle(context)

        val sortedSubtitlesLanguages = mutableSetOf<String>()

        // Cloudstream3 logic for unique subtitle names
        return sortedSubtitles.map { subtitle ->
            val subtitleName = subtitle.language.toUniqueSubtitleLanguage()

            val subtitleConfiguration = SubtitleConfiguration
                .Builder(Uri.parse(subtitle.url))
                .setMimeType(subtitle.toMimeType())
                .setLanguage(subtitleName)
                .build()

            sortedSubtitlesLanguages.add(subtitleName)
            availableSubtitles.add(subtitle.copy(language = subtitleName))

            SingleSampleMediaSource.Factory(
                when (subtitle.type) {
                    SubtitleSource.ONLINE -> okHttpDataSource
                    else -> localDataSource
                }
            ).createMediaSource(subtitleConfiguration, C.TIME_UNSET)
        }.toTypedArray()
    }

    /**
     * Callback function triggered when the subtitles are changed.
     *
     * @param index The index of the selected subtitle.
     */
    fun onSubtitleChange(index: Int) {
        selectedSubtitleIndex = index

        preferredSubtitleLanguage = availableSubtitles.getOrNull(selectedSubtitleIndex)?.language ?: return

        val oldTrackSelectionParameters = player?.trackSelectionParameters

        oldTrackSelectionParameters?.let {
            player?.trackSelectionParameters = when (selectedSubtitleIndex) {
                // 0 == OFF Subtitles
                0 -> {
                    it.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                }

                else -> {
                    it.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setPreferredTextLanguage(preferredSubtitleLanguage)
                        .build()
                }
            }
        }
    }

    /**
     * Callback function triggered when the audio track is changed.
     *
     * @param index The index of the selected audio track.
     */
    fun onAudioChange(index: Int) {
        selectedAudioIndex = index

        val audioTrack = audioTrackGroups.getOrNull(selectedAudioIndex)
            ?.getTrackFormat(0)
            ?.language
            ?: return

        preferredAudioLanguage = audioTrack
        player?.run {
            trackSelectionParameters =
                trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .setPreferredAudioLanguage(preferredAudioLanguage)
                    .build()
        }
    }

    /**
     * Callback function triggered when the audio track is changed.
     * Though, this one needs the shorthand language name to change
     * the audio.
     *
     * @param language The language of the audio track
     */
    fun onAudioChange(language: String) {
        val index = availableAudios.indexOfFirst {
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
        currentTextRenderer?.setRenderOffsetMs(offset)
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
            val style = CaptionStyleCompat(
                appSettings.subtitleColor,
                appSettings.subtitleBackgroundColor,
                Color.TRANSPARENT,
                appSettings.subtitleEdgeType.type,
                Color.BLACK,
                appSettings.subtitleFontStyle.typeface
            )

            val fontSize = if (isInPictureInPictureMode)
                FONT_SIZE_PIP_MODE
            else appSettings.subtitleSize.getDp(isInTv)

            setApplyEmbeddedFontSizes(false)
            setApplyEmbeddedStyles(false)
            setStyle(style)
            setFixedTextSize(
                /* unit = */ TypedValue.COMPLEX_UNIT_SP,
                /* size = */ fontSize
            )
        }
    }

    fun updateAppSettings(newAppSettings: AppSettings) {
        appSettings = newAppSettings
    }

    /**
     * Gets all supported formats in a list
     * */
    private fun List<Tracks.Group>.getFormats(): List<Format> {
        return this.map {
            it.getFormats()
        }.flatten()
    }

    private fun Tracks.Group.getFormats(): List<Format> {
        return (0 until this.mediaTrackGroup.length).mapNotNull { i ->
            if (this.isSupported)
                this.mediaTrackGroup.getFormat(i)
            else null
        }
    }

    private fun String.toUniqueSubtitleLanguage(): String {
        var language = this
        var count = 0
        while (availableSubtitles.fastAny { it.language == language }) {
            count++
            language = "$this $count"
        }

        return language
    }
}