package com.flixclusive.feature.mobile.player.preview

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
import androidx.media3.ui.SubtitleView
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.PiPEvent
import com.flixclusive.core.presentation.player.model.CacheMediaItem
import com.flixclusive.core.presentation.player.model.MediaItemKey
import com.flixclusive.core.presentation.player.model.track.MediaServer
import com.flixclusive.core.presentation.player.model.track.MediaSubtitle

@OptIn(UnstableApi::class)
@Stable
internal class PreviewPlayer(
    private val context: Context
) : AppPlayer {
    override var subtitleView: SubtitleView? = null
    override var currentSubtitleOffset by mutableLongStateOf(0L)

    private var exoPlayer: ExoPlayer? = null
    private var _playWhenReady: Boolean = true
    private val availableSubtitles = mutableListOf<MediaSubtitle>()

    override val currentCacheMediaItem: CacheMediaItem? = null

    override fun initialize() {
        if (exoPlayer != null) return

        exoPlayer = ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()

        val mediaItems = PreviewPlayerData.createMediaItems()
        exoPlayer?.setMediaItems(mediaItems)
        exoPlayer?.prepare()

        availableSubtitles.clear()
        availableSubtitles.addAll(PreviewPlayerData.sampleSubtitles)
    }

    override fun prepare(
        key: MediaItemKey,
        servers: List<MediaServer>,
        subtitles: List<MediaSubtitle>,
        startPositionMs: Long,
        playImmediately: Boolean
    ) {
        availableSubtitles.clear()
        availableSubtitles.addAll(subtitles)
        exoPlayer?.seekTo(startPositionMs)
        _playWhenReady = playImmediately
        exoPlayer?.playWhenReady = playImmediately
    }

    override fun prepare() {
        exoPlayer?.prepare()
    }

    override fun releaseMediaSession() {
    }

    override fun switchMediaSource(key: MediaItemKey): Boolean {
        return true
    }

    override fun selectServer(index: Int) {
    }

    override fun selectSubtitle(index: Int) {
    }

    override fun addSubtitle(subtitle: MediaSubtitle) {
        availableSubtitles.add(subtitle)
    }

    override fun selectAudio(index: Int) {
    }

    override fun changeSubtitleDelay(offset: Long) {
        currentSubtitleOffset = offset
    }

    override fun handlePiPEvent(event: Int) {
        when (event) {
            PiPEvent.PLAY.ordinal -> {
                play()
                _playWhenReady = true
            }
            PiPEvent.PAUSE.ordinal -> {
                pause()
                _playWhenReady = false
            }
            PiPEvent.REPLAY.ordinal -> {
                seekTo(0)
                _playWhenReady = true
            }
            PiPEvent.FORWARD.ordinal -> seekForward()
            PiPEvent.BACKWARD.ordinal -> seekBack()
        }
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

    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
        exoPlayer?.setMediaItems(mediaItems)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        exoPlayer?.setMediaItems(mediaItems, resetPosition)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) {
        exoPlayer?.setMediaItems(mediaItems, startIndex, startPositionMs)
    }

    override fun setMediaItem(mediaItem: MediaItem) {
        exoPlayer?.setMediaItem(mediaItem)
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        exoPlayer?.setMediaItem(mediaItem, startPositionMs)
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        exoPlayer?.setMediaItem(mediaItem, resetPosition)
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        exoPlayer?.addMediaItem(mediaItem)
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        exoPlayer?.addMediaItem(index, mediaItem)
    }

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        exoPlayer?.addMediaItems(mediaItems)
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        exoPlayer?.addMediaItems(index, mediaItems)
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        exoPlayer?.moveMediaItem(currentIndex, newIndex)
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        exoPlayer?.moveMediaItems(fromIndex, toIndex, newIndex)
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        exoPlayer?.replaceMediaItem(index, mediaItem)
    }

    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: MutableList<MediaItem>) {
        exoPlayer?.replaceMediaItems(fromIndex, toIndex, mediaItems)
    }

    override fun removeMediaItem(index: Int) {
        exoPlayer?.removeMediaItem(index)
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
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

    override fun play() {
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        _playWhenReady = playWhenReady
        exoPlayer?.playWhenReady = playWhenReady
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

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        exoPlayer?.seekTo(mediaItemIndex, positionMs)
    }

    override fun getSeekBackIncrement(): Long {
        return exoPlayer?.seekBackIncrement ?: 10_000L
    }

    override fun seekBack() {
        exoPlayer?.seekBack()
    }

    override fun getSeekForwardIncrement(): Long {
        return exoPlayer?.seekForwardIncrement ?: 10_000L
    }

    override fun seekForward() {
        exoPlayer?.seekForward()
    }

    override fun hasPreviousMediaItem(): Boolean {
        return exoPlayer?.hasPreviousMediaItem() ?: false
    }

    override fun seekToPreviousMediaItem() {
        exoPlayer?.seekToPreviousMediaItem()
    }

    override fun getMaxSeekToPreviousPosition(): Long {
        return exoPlayer?.maxSeekToPreviousPosition ?: 3_000L
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

    override fun release() {
        exoPlayer?.release()
        exoPlayer = null
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
        return exoPlayer?.isCurrentWindowSeekable ?: true
    }

    override fun isCurrentMediaItemSeekable(): Boolean {
        return exoPlayer?.isCurrentMediaItemSeekable ?: true
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

    @Deprecated("Deprecated in Java")
    override fun getDeviceVolume(): Int {
        return exoPlayer?.deviceVolume ?: 0
    }

    @Deprecated("Deprecated in Java")
    override fun isDeviceMuted(): Boolean {
        return exoPlayer?.isDeviceMuted ?: false
    }

    @Deprecated("Deprecated in Java")
    override fun setDeviceVolume(volume: Int) {
        exoPlayer?.deviceVolume = volume
    }

    override fun setDeviceVolume(volume: Int, flags: Int) {
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

    override fun setDeviceMuted(muted: Boolean, flags: Int) {
        exoPlayer?.setDeviceMuted(muted, flags)
    }

    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) {
        exoPlayer?.setAudioAttributes(audioAttributes, handleAudioFocus)
    }
}
