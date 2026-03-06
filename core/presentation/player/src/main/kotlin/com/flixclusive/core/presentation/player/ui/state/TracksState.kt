package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEachIndexed
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.R
import com.flixclusive.core.presentation.player.model.track.PlayerAudio
import com.flixclusive.core.presentation.player.model.track.PlayerSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource
import com.flixclusive.core.presentation.player.util.TracksUtil.getFormats
import com.flixclusive.core.presentation.player.util.TracksUtil.getIndexOfPreferredLanguage
import com.flixclusive.core.presentation.player.util.TracksUtil.getName
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.core.util.log.warnLog

/**
 * Holds information about available audio and subtitle tracks,
 * as well as the currently selected indices for each.
 * */
@UnstableApi
@Stable
class TracksState(
    private val player: AppPlayer,
    subtitlesPreferences: SubtitlesPreferences,
    playerPreferences: PlayerPreferences,
) {
    // We use this to determine when the media item has changed, so we can clear and re-extract tracks.
    private var lastInitializedMediaItem: String? = null

    val subtitles = mutableStateListOf<PlayerSubtitle>()
    private val addedSubtitles = mutableSetOf<PlayerSubtitle>()
    val audios = mutableStateListOf<PlayerAudio>()

    var preferredAudioLanguage by mutableStateOf(playerPreferences.audioLanguage)
        private set
    var preferredSubtitleLanguage by mutableStateOf(subtitlesPreferences.subtitleLanguage)
        private set

    var selectedSubtitle by mutableIntStateOf(0)
        private set

    var selectedAudio by mutableIntStateOf(0)
        private set

    internal suspend fun observe() {
        player.listen { events ->
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) && currentMediaItem?.mediaId != lastInitializedMediaItem) {
                lastInitializedMediaItem = null
                audios.clear()
                subtitles.clear()
            }

            if (events.contains(Player.EVENT_TRACKS_CHANGED) && lastInitializedMediaItem == null) {
                lastInitializedMediaItem = player.currentMediaItem?.mediaId
                extractAudios()
                extractSubtitles()
                selectDefaultTracks()
            }
        }
    }

    private fun extractAudios() {
        val type = C.TRACK_TYPE_AUDIO
        val formats = getTrackFormats(type)
        if (formats.isEmpty()) return

        audios.clear()
        formats.fastForEachIndexed { i, format ->
            val name = format.getName(type, i)
            infoLog("Found audio track ${i + 1}: ${format.getName(type, i)}")

            audios.add(name)
        }
    }

    private fun extractSubtitles() {
        val type = C.TRACK_TYPE_TEXT
        val trackGroups = getTrackFormats(type)
        if (trackGroups.isEmpty()) return

        subtitles.clear()
        addOffSubtitle()
        subtitles.addAll(addedSubtitles)

        infoLog("Extracted ${audios.size} subtitles!")
        trackGroups.fastForEachIndexed { i, format ->
            val source = if (format.id?.contains("http", ignoreCase = true) == true) {
                TrackSource.REMOTE
            } else {
                TrackSource.EMBEDDED
            }

            subtitles.add(
                PlayerSubtitle(
                    url = format.id ?: "",
                    label = format.getName(type, i),
                    source = source
                )
            )
        }
    }

    private fun selectDefaultTracks() {
        val subtitleIndex = getIndexOfPreferredLanguage(
            list = subtitles,
            preferredLanguage = preferredSubtitleLanguage,
            languageProvider = { it.label },
        )

        val audioIndex = getIndexOfPreferredLanguage(
            list = audios,
            preferredLanguage = preferredAudioLanguage,
            languageProvider = { it },
        )

        onSubtitleSelect(subtitleIndex)
        onAudioSelect(audioIndex)
    }

    fun resetTracks() {
        audios.clear()
        subtitles.clear()
        addedSubtitles.clear()
        lastInitializedMediaItem = null
    }

    fun onAddSubtitle(subtitle: PlayerSubtitle) {
        val success = player.addSubtitle(subtitle)
        if (!success) {
            warnLog("Failed to add subtitle: ${subtitle.label}")
            player._errors.tryEmit(UiText.from(R.string.failed_to_add_subtitle))
            return
        }

        addedSubtitles.add(subtitle)
        if (!subtitles.contains(subtitle)) {
            subtitles.add(subtitle)
        }

        onSubtitleSelect(subtitles.size - 1)
    }

    fun onSubtitleSelect(index: Int) {
        selectedSubtitle = index
        val subtitle = subtitles.getOrNull(index)

        preferredSubtitleLanguage = subtitle?.label.orEmpty()
        player.clearCues()
        player.selectSubtitle(index - if (hasOffSubtitle()) 1 else 0)
    }

    fun onAudioSelect(index: Int) {
        selectedAudio = index
        val audio = audios.getOrNull(index)

        preferredAudioLanguage = audio.orEmpty()
        player.selectAudio(index)
    }

    private fun hasOffSubtitle(): Boolean {
        return subtitles.getOrNull(0)?.label == "Off"
    }

    private fun addOffSubtitle() {
        subtitles.add(
            0,
            PlayerSubtitle(
                url = "",
                label = "Off",
                source = TrackSource.EMBEDDED
            )
        )
    }

    private fun getTrackFormats(type: Int): List<Format> {
        return player.currentTracks.groups.fastFilter {
            it.type == type && it.isSupported
        }.getFormats()
    }

    companion object {
        /**
         * Remembers and initializes a [TracksState] for managing audio and subtitle tracks in a media player.
         * */
        @Composable
        fun rememberTracksState(
            player: AppPlayer,
            subtitlesPreferences: SubtitlesPreferences,
            playerPreferences: PlayerPreferences,
        ): TracksState {
            val state = remember(player, subtitlesPreferences, playerPreferences) {
                TracksState(player, subtitlesPreferences, playerPreferences)
            }

            LaunchedEffect(
                player,
                subtitlesPreferences.subtitleLanguage,
                playerPreferences.audioLanguage,
            ) { state.observe() }

            return state
        }
    }
}
