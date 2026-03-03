package com.flixclusive.core.presentation.player

import android.content.Context
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.SubtitleView
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.player.extensions.isLiveError
import com.flixclusive.core.presentation.player.extensions.isNetworkException
import com.flixclusive.core.presentation.player.extensions.setStyle
import com.flixclusive.core.presentation.player.extensions.switchTrack
import com.flixclusive.core.presentation.player.model.CacheMediaItem
import com.flixclusive.core.presentation.player.model.CueWithTiming
import com.flixclusive.core.presentation.player.model.MediaItemKey
import com.flixclusive.core.presentation.player.model.track.MediaServer
import com.flixclusive.core.presentation.player.model.track.MediaServer.Companion.getIndexOfPreferredQuality
import com.flixclusive.core.presentation.player.model.track.MediaSubtitle
import com.flixclusive.core.presentation.player.ui.PiPEvent
import com.flixclusive.core.presentation.player.util.PlayerBuilderHelper.disableSSLVerification
import com.flixclusive.core.presentation.player.util.PlayerBuilderHelper.getLoadControl
import com.flixclusive.core.presentation.player.util.PlayerBuilderHelper.getRenderers
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog

private const val PLAYER_TOLERANCE_BEFORE_US = 300_000L
private const val PLAYER_TOLERANCE_AFTER_US = 300_000L

/**
 * A wrapper class for [Player] functionalities.
 *
 * This class is intended to be used for Jetpack Compose UIs.
 */
@Suppress("DEPRECATION")
@OptIn(UnstableApi::class)
@Stable
class AppPlayer(
    private val context: Context,
    private val dataSourceFactory: AppDataSourceFactory,
    private val playerPrefs: PlayerPreferences,
    private val subtitlePrefs: SubtitlesPreferences,
    errorReceiver: PlayerErrorReceiver,
) : CuesProvider, Player {
    override var offset by mutableLongStateOf(0L)
        private set

    val currentCuesWithTiming = mutableStateListOf<CueWithTiming>()

    var subtitleView: SubtitleView? = null

    private var textRenderer: TextRenderer? = null

    /** Only internally visible so ComposePlayer component can access it */
    internal var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val listener = InternalPlayerListener(errorReceiver)

    /** Backing property for playWhenReady to keep the value when the player is null. */
    private var _playWhenReady: Boolean = true

    private val mediaSourceManager: MediaSourceManager by lazy {
        MediaSourceManager(dataSourceFactory)
    }
    val currentCacheMediaItem: CacheMediaItem?
        get() {
            return mediaSourceManager.getCurrentMediaItem()
        }

    fun initialize() {
        if (exoPlayer != null && mediaSession != null) return

        if (exoPlayer == null) {
            infoLog("Initializing the player...")
            disableSSLVerification()

            val trackSelector = DefaultTrackSelector(context)
            val loadControl = getLoadControl(
                bufferCacheSize = playerPrefs.bufferCacheSize,
                videoBufferMs = playerPrefs.videoBufferMs,
            )

            exoPlayer = ExoPlayer
                .Builder(context)
                .apply {
                    setDeviceVolumeControlEnabled(true)
                    setTrackSelector(trackSelector)
                    setMediaSourceFactory(
                        DefaultMediaSourceFactory(context)
                            .setDataSourceFactory(dataSourceFactory.remote),
                    )
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
                            cuesProvider = this@AppPlayer,
                            decoderPriority = playerPrefs.decoderPriority,
                            onTextRendererChange = { textRenderer = it },
                        )
                    }
                    playerPrefs.seekAmount.let {
                        setSeekBackIncrementMs(it)
                        setSeekForwardIncrementMs(it)
                    }
                    setSeekParameters(SeekParameters(PLAYER_TOLERANCE_BEFORE_US, PLAYER_TOLERANCE_AFTER_US))
                    setLoadControl(loadControl)

                    val playbackAttributes = AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build()

                    setAudioAttributes(playbackAttributes, true)
                }.build()
                .apply {
                    setHandleAudioBecomingNoisy(true)
                    addListener(listener)
                }
        }

        infoLog("Initializing the media session...")
        mediaSession = MediaSession.Builder(context, exoPlayer!!).build()

        subtitleView?.let {
            infoLog("Initializing SubtitleView...")
            it.setCues(null)

            if (isCommandAvailable(Player.COMMAND_GET_TEXT)) {
                it.setCues(currentCues.cues)
            }
        }

        infoLog("Player initialized successfully!")
    }

    fun prepare(
        key: MediaItemKey,
        servers: List<MediaServer>,
        subtitles: List<MediaSubtitle>,
        startPositionMs: Long,
        playImmediately: Boolean,
    ) {
        if (exoPlayer == null) return

        val mediaSource: MediaSource
        var cacheMediaItem = mediaSourceManager.getCacheMediaItem(key)
        if (cacheMediaItem == null) {
            val streamIndex = servers.getIndexOfPreferredQuality(playerPrefs.quality)

            mediaSource = mediaSourceManager.createMediaSource(
                server = servers[streamIndex],
                subtitles = subtitles,
            )

            cacheMediaItem = CacheMediaItem(
                mediaSource = mediaSource,
                servers = servers,
                subtitles = subtitles,
                currentServerIndex = streamIndex,
            )

            mediaSourceManager.setCacheMediaItem(key = key, cacheMediaItem = cacheMediaItem)
        } else {
            mediaSource = cacheMediaItem.mediaSource
        }

        if (playImmediately) {
            infoLog("Preparing the player...")
            mediaSourceManager.setCurrentKey(key)

            // Update data source headers
            val selectedStreamIndex = cacheMediaItem.currentServerIndex
            val selectedStream = cacheMediaItem.servers[selectedStreamIndex]
            selectedStream.headers?.let {
                dataSourceFactory.setRequestProperties(it)
            }

            seekTo(selectedStreamIndex, startPositionMs)
            exoPlayer?.setMediaSource(mediaSource)
            prepare()
            playWhenReady = _playWhenReady
        }
    }

    fun releaseMediaSession() {
        mediaSession?.release()
        mediaSession = null
    }

    override fun release() {
        infoLog("Releasing...")
        removeListener(listener)
        exoPlayer?.release()
        exoPlayer = null
        textRenderer = null
    }

    fun switchMediaSource(key: MediaItemKey, startPositionMs: Long = C.TIME_UNSET): Boolean {
        if (exoPlayer == null) return false

        val cacheMediaItem = mediaSourceManager.getCacheMediaItem(key) ?: return false
        mediaSourceManager.setCurrentKey(key)

        // Set new media source
        exoPlayer!!.setMediaSource(cacheMediaItem.mediaSource)
        exoPlayer!!.prepare()
        exoPlayer!!.seekTo(startPositionMs)
        playWhenReady = _playWhenReady

        return true
    }

    fun hasMediaSource(key: MediaItemKey): Boolean {
        return mediaSourceManager.getCacheMediaItem(key) != null
    }

    fun selectServer(index: Int) {
        mediaSourceManager.switchStreamIndex(index)

        val cacheMediaItem = mediaSourceManager.getCurrentMediaItem()
            ?: throw IllegalStateException("No cached media item found for the current key.")

        val server = cacheMediaItem.servers[index]
        val mediaItem = mediaSourceManager.createMediaItem(server.url)

        exoPlayer?.let {
            val currentMediaItemIndex = it.currentMediaItemIndex
            it.replaceMediaItem(currentMediaItemIndex, mediaItem)
        }
    }

    fun selectSubtitle(index: Int) {
        switchTrack(C.TRACK_TYPE_TEXT, index)
    }

    fun selectAudio(index: Int) {
        switchTrack(C.TRACK_TYPE_AUDIO, index)
    }

    fun addSubtitle(subtitle: MediaSubtitle) {
        val wasAdded = mediaSourceManager.addSubtitle(subtitle)

        // If subtitle was added successfully, we need to re-prepare the player
        if (wasAdded) {
            exoPlayer?.prepare()
        }
    }

    fun changeSubtitleDelay(offset: Long) {
        this.offset = offset

        // Apply the offset change immediately to the current text renderer
        if (textRenderer?.state == TextRenderer.STATE_ENABLED ||
            textRenderer?.state == TextRenderer.STATE_STARTED
        ) {
            // Force the text renderer to re-render with the new offset
            // by resetting its position to the current playback position
            val currentPos = exoPlayer?.currentPosition ?: 0L

            // The renderer will pick up the new offset from the updated state
            // when it re-renders the current position
            try {
                textRenderer?.resetPosition(
                    currentPos,
                    false
                )
            } catch (e: Exception) {
                errorLog(e)
                val seekPos = (currentPos - 50).coerceAtLeast(0L)
                seekTo(seekPos)
                seekTo(currentPos)
            }
        }
    }

    fun handlePiPEvent(event: Int) {
        when (event) {
            PiPEvent.PLAY.ordinal -> {
                play()
                _playWhenReady = true
            }

            PiPEvent.PAUSE.ordinal -> {
                pause()
                _playWhenReady = false
            }

            PiPEvent.FORWARD.ordinal -> seekForward()
            PiPEvent.BACKWARD.ordinal -> seekBack()
        }
    }

    fun setSubtitleStyle(isInPipMode: Boolean) {
        subtitleView?.setStyle(
            isInPipMode = isInPipMode,
            subtitlePrefs = subtitlePrefs
        )
    }

    override fun setPlayWhenReady(value: Boolean) {
        _playWhenReady = value
        exoPlayer?.playWhenReady = value
    }

    override fun addCue(cue: CueWithTiming) {
        currentCuesWithTiming.add(cue)
    }

    override fun clearCues() {
        currentCuesWithTiming.clear()
    }

    private inner class InternalPlayerListener(
        private val errorReceiver: PlayerErrorReceiver,
    ) : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                changeSubtitleDelay(0)
                return
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            errorReceiver.onPlayerError(error)
            val isDurationNotUnset = exoPlayer?.duration != null && exoPlayer?.duration != C.TIME_UNSET

            errorLog(error.stackTraceToString())
            if (error.isNetworkException() && isDurationNotUnset || error.isLiveError()) {
                seekToDefaultPosition()
                prepare()
                playWhenReady = _playWhenReady
                return
            }

            mediaSourceManager.markStreamAsFailed(currentMediaItemIndex)

            val nextIndex = mediaSourceManager.getNextAvailableStreamIndex(currentMediaItemIndex)
            if (nextIndex == null) {
                errorLog("All servers have failed or no alternative servers available.")
                return
            }

            seekTo(nextIndex, currentPosition)
            prepare()
        }

        override fun onCues(cueGroup: CueGroup) {
            subtitleView?.setCues(cueGroup.cues)
        }
    }

    companion object {
        val playbackSpeedRange = 0.15f..5.0f

        /**
         * Calculates 15% of the [duration] and returns the remaining time.
         * This is used to determine when 80% of the video has been watched.
         *
         * @param duration Total duration in milliseconds.
         *
         * @return The threshold time in milliseconds.
         * */
        internal fun getCompletionThreshold(duration: Long): Long {
            val deductedAmount = (duration * 0.85).toLong()
            return duration - deductedAmount
        }

        /**
         * Checks if the [progress] is within the [threshold] of the [duration].
         * Default threshold is 10 seconds (10,000 milliseconds).
         *
         * @param progress Current progress in milliseconds.
         * @param duration Total duration in milliseconds.
         * @param threshold Threshold in milliseconds to check against.
         *
         * @return True if within threshold, false otherwise.
         * */
        internal fun isInThreshold(
            progress: Long,
            duration: Long,
            threshold: Long = 10_000L,
        ) = (duration - progress) <= threshold
    }

    // MARK: Don't scroll down anymore pls

    override fun play() {
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    override fun seekBack() {
        exoPlayer?.seekBack()
    }

    override fun seekForward() {
        exoPlayer?.seekForward()
    }

    override fun prepare() {
        exoPlayer?.prepare()
    }

    override fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
    ) {
        exoPlayer?.seekTo(mediaItemIndex, positionMs)
    }

    override fun getApplicationLooper(): Looper {
        return exoPlayer?.applicationLooper ?: Looper.getMainLooper()
    }

    override fun addListener(listener: Player.Listener) {
        exoPlayer?.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        exoPlayer?.removeListener(listener)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>) {
        exoPlayer?.setMediaItems(mediaItems)
    }

    override fun setMediaItems(
        mediaItems: List<MediaItem>,
        resetPosition: Boolean,
    ) {
        exoPlayer?.setMediaItems(mediaItems, resetPosition)
    }

    override fun setMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        exoPlayer?.setMediaItems(mediaItems, startIndex, startPositionMs)
    }

    override fun setMediaItem(mediaItem: MediaItem) {
        exoPlayer?.setMediaItem(mediaItem)
    }

    override fun setMediaItem(
        mediaItem: MediaItem,
        startPositionMs: Long,
    ) {
        exoPlayer?.setMediaItem(mediaItem, startPositionMs)
    }

    override fun setMediaItem(
        mediaItem: MediaItem,
        resetPosition: Boolean,
    ) {
        exoPlayer?.setMediaItem(mediaItem, resetPosition)
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        exoPlayer?.addMediaItem(mediaItem)
    }

    override fun addMediaItem(
        index: Int,
        mediaItem: MediaItem,
    ) {
        exoPlayer?.addMediaItem(index, mediaItem)
    }

    override fun addMediaItems(mediaItems: List<MediaItem>) {
        exoPlayer?.addMediaItems(mediaItems)
    }

    override fun addMediaItems(
        index: Int,
        mediaItems: List<MediaItem>,
    ) {
        exoPlayer?.addMediaItems(index, mediaItems)
    }

    override fun moveMediaItem(
        currentIndex: Int,
        newIndex: Int,
    ) {
        exoPlayer?.moveMediaItem(currentIndex, newIndex)
    }

    override fun moveMediaItems(
        fromIndex: Int,
        toIndex: Int,
        newIndex: Int,
    ) {
        exoPlayer?.moveMediaItems(fromIndex, toIndex, newIndex)
    }

    override fun replaceMediaItem(
        index: Int,
        mediaItem: MediaItem,
    ) {
        exoPlayer?.replaceMediaItem(index, mediaItem)
    }

    override fun replaceMediaItems(
        fromIndex: Int,
        toIndex: Int,
        mediaItems: List<MediaItem>,
    ) {
        exoPlayer?.replaceMediaItems(fromIndex, toIndex, mediaItems)
    }

    override fun removeMediaItem(index: Int) {
        exoPlayer?.removeMediaItem(index)
    }

    override fun removeMediaItems(
        fromIndex: Int,
        toIndex: Int,
    ) {
        exoPlayer?.removeMediaItems(fromIndex, toIndex)
    }

    override fun clearMediaItems() {
        exoPlayer?.clearMediaItems()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return exoPlayer?.isCommandAvailable(command) ?: false
    }

    override fun canAdvertiseSession(): Boolean {
        return exoPlayer?.canAdvertiseSession() ?: false
    }

    override fun getAvailableCommands(): Player.Commands {
        return exoPlayer?.availableCommands ?: Player.Commands.EMPTY
    }

    override fun getPlaybackState(): Int {
        return exoPlayer?.playbackState ?: Player.STATE_IDLE
    }

    override fun getPlaybackSuppressionReason(): Int {
        return exoPlayer?.playbackSuppressionReason ?: Player.PLAYBACK_SUPPRESSION_REASON_NONE
    }

    override fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    override fun getPlayerError(): PlaybackException? {
        return exoPlayer?.playerError
    }

    override fun getPlayWhenReady(): Boolean {
        return exoPlayer?.playWhenReady ?: false
    }

    override fun setRepeatMode(repeatMode: Int) {
        exoPlayer?.repeatMode = repeatMode
    }

    override fun getRepeatMode(): Int {
        return exoPlayer?.repeatMode ?: Player.REPEAT_MODE_OFF
    }

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        exoPlayer?.shuffleModeEnabled = shuffleModeEnabled
    }

    override fun getShuffleModeEnabled(): Boolean {
        return exoPlayer?.shuffleModeEnabled ?: false
    }

    override fun isLoading(): Boolean {
        return exoPlayer?.isLoading ?: false
    }

    override fun seekToDefaultPosition() {
        exoPlayer?.seekToDefaultPosition()
    }

    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        exoPlayer?.seekToDefaultPosition(mediaItemIndex)
    }

    override fun getSeekBackIncrement(): Long {
        return exoPlayer?.seekBackIncrement ?: 0L
    }

    override fun getSeekForwardIncrement(): Long {
        return exoPlayer?.seekForwardIncrement ?: 0L
    }

    override fun hasPreviousMediaItem(): Boolean {
        return exoPlayer?.hasPreviousMediaItem() ?: false
    }

    override fun seekToPreviousMediaItem() {
        exoPlayer?.seekToPreviousMediaItem()
    }

    override fun getMaxSeekToPreviousPosition(): Long {
        return exoPlayer?.maxSeekToPreviousPosition ?: 0L
    }

    override fun seekToPrevious() {
        exoPlayer?.seekToPrevious()
    }

    override fun hasNextMediaItem(): Boolean {
        return exoPlayer?.hasNextMediaItem() ?: false
    }

    override fun seekToNextMediaItem() {
        exoPlayer?.seekToNextMediaItem()
    }

    override fun seekToNext() {
        exoPlayer?.seekToNext()
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        exoPlayer?.playbackParameters = playbackParameters
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
    }

    override fun getPlaybackParameters(): PlaybackParameters {
        return exoPlayer?.playbackParameters ?: PlaybackParameters.DEFAULT
    }

    override fun stop() {
        exoPlayer?.stop()
    }

    override fun getCurrentTracks(): Tracks {
        return exoPlayer?.currentTracks ?: Tracks.EMPTY
    }

    override fun getTrackSelectionParameters(): TrackSelectionParameters {
        return exoPlayer?.trackSelectionParameters ?: TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
    }

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
        exoPlayer?.trackSelectionParameters = parameters
    }

    override fun getMediaMetadata(): MediaMetadata {
        return exoPlayer?.mediaMetadata ?: MediaMetadata.EMPTY
    }

    override fun getPlaylistMetadata(): MediaMetadata {
        return exoPlayer?.playlistMetadata ?: MediaMetadata.EMPTY
    }

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        exoPlayer?.playlistMetadata = mediaMetadata
    }

    override fun getCurrentManifest(): Any? {
        return exoPlayer?.currentManifest
    }

    override fun getCurrentTimeline(): Timeline {
        return exoPlayer?.currentTimeline ?: Timeline.EMPTY
    }

    override fun getCurrentPeriodIndex(): Int {
        return exoPlayer?.currentPeriodIndex ?: 0
    }

    @Deprecated("Deprecated in Java")
    override fun getCurrentWindowIndex(): Int {
        return exoPlayer?.currentWindowIndex ?: 0
    }

    override fun getCurrentMediaItemIndex(): Int {
        return exoPlayer?.currentMediaItemIndex ?: 0
    }

    @Deprecated("Deprecated in Java")
    override fun getNextWindowIndex(): Int {
        return exoPlayer?.nextWindowIndex ?: C.INDEX_UNSET
    }

    override fun getNextMediaItemIndex(): Int {
        return exoPlayer?.nextMediaItemIndex ?: C.INDEX_UNSET
    }

    @Deprecated("Deprecated in Java")
    override fun getPreviousWindowIndex(): Int {
        return exoPlayer?.previousWindowIndex ?: C.INDEX_UNSET
    }

    override fun getPreviousMediaItemIndex(): Int {
        return exoPlayer?.previousMediaItemIndex ?: C.INDEX_UNSET
    }

    override fun getCurrentMediaItem(): MediaItem? {
        return exoPlayer?.currentMediaItem
    }

    override fun getMediaItemCount(): Int {
        return exoPlayer?.mediaItemCount ?: 0
    }

    override fun getMediaItemAt(index: Int): MediaItem {
        return exoPlayer?.getMediaItemAt(index) ?: MediaItem.EMPTY
    }

    override fun getDuration(): Long {
        return exoPlayer?.duration ?: C.TIME_UNSET
    }

    override fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    override fun getBufferedPosition(): Long {
        return exoPlayer?.bufferedPosition ?: 0L
    }

    override fun getBufferedPercentage(): Int {
        return exoPlayer?.bufferedPercentage ?: 0
    }

    override fun getTotalBufferedDuration(): Long {
        return exoPlayer?.totalBufferedDuration ?: 0L
    }

    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowDynamic(): Boolean {
        return exoPlayer?.isCurrentWindowDynamic ?: false
    }

    override fun isCurrentMediaItemDynamic(): Boolean {
        return exoPlayer?.isCurrentMediaItemDynamic ?: false
    }

    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowLive(): Boolean {
        return exoPlayer?.isCurrentWindowLive ?: false
    }

    override fun isCurrentMediaItemLive(): Boolean {
        return exoPlayer?.isCurrentMediaItemLive ?: false
    }

    override fun getCurrentLiveOffset(): Long {
        return exoPlayer?.currentLiveOffset ?: C.TIME_UNSET
    }

    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowSeekable(): Boolean {
        return exoPlayer?.isCurrentWindowSeekable ?: false
    }

    override fun isCurrentMediaItemSeekable(): Boolean {
        return exoPlayer?.isCurrentMediaItemSeekable ?: false
    }

    override fun isPlayingAd(): Boolean {
        return exoPlayer?.isPlayingAd ?: false
    }

    override fun getCurrentAdGroupIndex(): Int {
        return exoPlayer?.currentAdGroupIndex ?: C.INDEX_UNSET
    }

    override fun getCurrentAdIndexInAdGroup(): Int {
        return exoPlayer?.currentAdIndexInAdGroup ?: C.INDEX_UNSET
    }

    override fun getContentDuration(): Long {
        return exoPlayer?.contentDuration ?: C.TIME_UNSET
    }

    override fun getContentPosition(): Long {
        return exoPlayer?.contentPosition ?: 0L
    }

    override fun getContentBufferedPosition(): Long {
        return exoPlayer?.contentBufferedPosition ?: 0L
    }

    override fun getAudioAttributes(): AudioAttributes {
        return exoPlayer?.audioAttributes ?: AudioAttributes.DEFAULT
    }

    override fun setVolume(volume: Float) {
        exoPlayer?.volume = volume
    }

    override fun getVolume(): Float {
        return exoPlayer?.volume ?: 1f
    }

    override fun clearVideoSurface() {
        exoPlayer?.clearVideoSurface()
    }

    override fun clearVideoSurface(surface: Surface?) {
        exoPlayer?.clearVideoSurface(surface)
    }

    override fun setVideoSurface(surface: Surface?) {
        exoPlayer?.setVideoSurface(surface)
    }

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        exoPlayer?.setVideoSurfaceHolder(surfaceHolder)
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        exoPlayer?.clearVideoSurfaceHolder(surfaceHolder)
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        exoPlayer?.setVideoSurfaceView(surfaceView)
    }

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
        exoPlayer?.clearVideoSurfaceView(surfaceView)
    }

    override fun setVideoTextureView(textureView: TextureView?) {
        exoPlayer?.setVideoTextureView(textureView)
    }

    override fun clearVideoTextureView(textureView: TextureView?) {
        exoPlayer?.clearVideoTextureView(textureView)
    }

    override fun getVideoSize(): VideoSize {
        return exoPlayer?.videoSize ?: VideoSize.UNKNOWN
    }

    override fun getSurfaceSize(): Size {
        return exoPlayer?.surfaceSize ?: Size.UNKNOWN
    }

    override fun getCurrentCues(): CueGroup {
        return exoPlayer?.currentCues ?: CueGroup.EMPTY_TIME_ZERO
    }

    override fun getDeviceInfo(): DeviceInfo {
        return exoPlayer?.deviceInfo ?: DeviceInfo.UNKNOWN
    }

    override fun getDeviceVolume(): Int {
        return exoPlayer?.deviceVolume ?: 0
    }

    override fun isDeviceMuted(): Boolean {
        return exoPlayer?.isDeviceMuted ?: false
    }

    @Deprecated("Deprecated in Java")
    override fun setDeviceVolume(volume: Int) {
        exoPlayer?.deviceVolume = volume
    }

    override fun setDeviceVolume(
        volume: Int,
        flags: Int,
    ) {
        exoPlayer?.setDeviceVolume(volume, flags)
    }

    @Deprecated("Deprecated in Java")
    override fun increaseDeviceVolume() {
        exoPlayer?.increaseDeviceVolume()
    }

    override fun increaseDeviceVolume(flags: Int) {
        exoPlayer?.increaseDeviceVolume(flags)
    }

    @Deprecated("Deprecated in Java")
    override fun decreaseDeviceVolume() {
        exoPlayer?.decreaseDeviceVolume()
    }

    override fun decreaseDeviceVolume(flags: Int) {
        exoPlayer?.decreaseDeviceVolume(flags)
    }

    @Deprecated("Deprecated in Java")
    override fun setDeviceMuted(muted: Boolean) {
        exoPlayer?.isDeviceMuted = muted
    }

    override fun setDeviceMuted(
        muted: Boolean,
        flags: Int,
    ) {
        exoPlayer?.setDeviceMuted(muted, flags)
    }

    override fun setAudioAttributes(
        audioAttributes: AudioAttributes,
        handleAudioFocus: Boolean,
    ) {
        exoPlayer?.setAudioAttributes(audioAttributes, handleAudioFocus)
    }

    override fun mute() {
        exoPlayer?.mute()
    }

    override fun unmute() {
        exoPlayer?.unmute()
    }
}
