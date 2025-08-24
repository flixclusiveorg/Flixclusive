package com.flixclusive.core.presentation.player.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.media3.common.Tracks
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.player.InternalPlayerImpl
import com.flixclusive.core.presentation.player.util.extensions.switchTrack
import com.flixclusive.core.presentation.player.util.internal.SubtitleUtil.getSubtitleSource
import com.flixclusive.core.presentation.player.util.internal.TracksUtil.getFormats
import com.flixclusive.core.presentation.player.util.internal.TracksUtil.getIndexOfPreferredLanguage
import com.flixclusive.core.presentation.player.util.internal.TracksUtil.getName
import com.flixclusive.core.util.log.infoLog
import com.flixclusive.model.provider.link.Subtitle

/**
 * Holds information about available audio and subtitle tracks,
 * as well as the currently selected indices for each.
 * */
@UnstableApi
class TracksState(
    private val player: Player,
    subtitlesPreferences: SubtitlesPreferences,
    playerPreferences: PlayerPreferences,
) {
    private val audioTrackGroups = mutableListOf<Tracks.Group>()

    val subtitles = mutableStateListOf<Subtitle>()
    val audios = mutableStateListOf<String>()

    var preferredAudioLanguage by mutableStateOf(playerPreferences.audioLanguage)
        private set
    var preferredSubtitleLanguage by mutableStateOf(subtitlesPreferences.subtitleLanguage)
        private set

    var selectedSubtitle by mutableIntStateOf(
        getIndexOfPreferredLanguage(
            list = subtitles,
            preferredLanguage = preferredSubtitleLanguage,
            languageProvider = { it.language },
        ),
    )
        private set

    var selectedAudio by mutableIntStateOf(
        getIndexOfPreferredLanguage(
            list = audios,
            preferredLanguage = preferredAudioLanguage,
            languageProvider = { it },
        ),
    )
        private set

    internal suspend fun observe() {
        val internalPlayer = (player as InternalPlayerImpl)
        internalPlayer.listen { events ->
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)
                && playbackState == Player.STATE_READY
                && internalPlayer.areTracksInitialized) {
                extractAudios()
                extractSubtitles()
                selectDefaultTracks()
                internalPlayer.areTracksInitialized = true
            }
        }
    }

    private fun extractAudios() {
        val trackGroups = player.currentTracks.groups.fastFilter {
            it.type == C.TRACK_TYPE_AUDIO && it.isSupported
        }

        audioTrackGroups.clear()
        audioTrackGroups.addAll(trackGroups)

        val audios = mutableListOf<String>()
        trackGroups.getFormats().fastForEachIndexed { i, format ->
            val name = format.getName(C.TRACK_TYPE_AUDIO, i)
            audios.add(name)
        }

        infoLog("Extracted ${audios.size} audios!")
    }

    private fun extractSubtitles() {
        val subtitleTrackGroups = player.currentTracks.groups.fastFilter { group ->
            group.type == C.TRACK_TYPE_TEXT && group.isSupported
        }

        subtitleTrackGroups.getFormats().fastForEachIndexed { i, format ->
            if (format.id == null) return@fastForEachIndexed

            subtitles.add(
                Subtitle(
                    url = format.id!!,
                    language = format.getName(C.TRACK_TYPE_TEXT, i),
                    type = getSubtitleSource(format.id!!),
                ),
            )
        }

        subtitles.clear()
        subtitles.addAll(subtitles)
        infoLog("Extracted ${subtitles.size - 1} subtitles!")
    }

    private fun selectDefaultTracks() {
        val subtitleIndex = getIndexOfPreferredLanguage(
            list = subtitles,
            preferredLanguage = preferredSubtitleLanguage,
            languageProvider = { it.language },
        )

        val audioIndex = getIndexOfPreferredLanguage(
            list = audios,
            preferredLanguage = preferredAudioLanguage,
            languageProvider = { it },
        )

        selectSubtitle(subtitleIndex)
        selectAudio(audioIndex)
    }

    internal fun selectSubtitle(index: Int) {
        if (index in subtitles.indices) return

        selectedSubtitle = index
        preferredSubtitleLanguage = if (index == -1) "" else subtitles[index].language
        player.switchTrack(C.TRACK_TYPE_TEXT, index)
    }

    internal fun selectAudio(index: Int) {
        if (index in audios.indices && index in audioTrackGroups.indices) return

        val unformattedLanguage = audioTrackGroups[index].getTrackFormat(0).language ?: return

        selectedAudio = index
        preferredAudioLanguage = unformattedLanguage
        player.switchTrack(C.TRACK_TYPE_AUDIO, index)
    }

    companion object {
        /**
         * Remembers and initializes a [TracksState] for managing audio and subtitle tracks in a media player.
         * */
        @Composable
        fun rememberTracksState(
            player: Player,
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
