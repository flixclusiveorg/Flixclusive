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
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.model.track.MediaAudio
import com.flixclusive.core.presentation.player.model.track.MediaSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource
import com.flixclusive.core.presentation.player.util.TracksUtil.getFormats
import com.flixclusive.core.presentation.player.util.TracksUtil.getIndexOfPreferredLanguage
import com.flixclusive.core.presentation.player.util.TracksUtil.getName
import com.flixclusive.core.util.log.infoLog

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
    val subtitles = mutableStateListOf<MediaSubtitle>()
    val audios = mutableStateListOf<MediaAudio>()

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
            // We only want to extract tracks once when they are first available.
            // Subsequent changes to track selection parameters don't require re-extraction.
            // TODO: Check if changing tracks in the player causes EVENT_TRACKS_CHANGED to be fired again.
            //       if yes, check if EVENT_TRACK_SELECTION_PARAMETERS_CHANGED is also fired.
            if (events.contains(Player.EVENT_TRACKS_CHANGED)) {
                extractAudios()
                extractSubtitles()
                selectDefaultTracks()
            }
        }
    }

    private fun extractAudios() {
        val trackGroups = player.currentTracks.groups.fastFilter {
            it.type == C.TRACK_TYPE_AUDIO && it.isSupported
        }

        audios.clear()
        trackGroups.getFormats().fastForEachIndexed { i, format ->
            val name = format.getName(C.TRACK_TYPE_AUDIO, i)
            audios.add(name)
        }

        infoLog("Extracted ${audios.size} audios!")
    }

    private fun extractSubtitles() {
        val currentMediaItem = player.currentCacheMediaItem ?: return
        val availableSubs = currentMediaItem.subtitles

        subtitles.clear()

        if (availableSubs.isNotEmpty()) {
            subtitles.add(
                MediaSubtitle(
                    url = "flixclusive_off_subtitle",
                    label = "Off",
                    source = TrackSource.EMBEDDED
                )
            )
        }

        subtitles.addAll(availableSubs)
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

        selectedSubtitle = subtitleIndex
        player.selectSubtitle(subtitleIndex - if (hasOffSubtitle()) 1 else 0)

        selectedAudio = audioIndex
        player.selectAudio(audioIndex)
    }

    fun onAddSubtitle(subtitle: MediaSubtitle) {
        player.addSubtitle(subtitle = subtitle)
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
