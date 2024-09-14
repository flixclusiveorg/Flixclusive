package com.flixclusive.feature.mobile.player

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.ui.player.BasePlayerViewModel
import com.flixclusive.core.ui.player.PlayerScreenNavArgs
import com.flixclusive.core.ui.player.PlayerSnackbarMessage
import com.flixclusive.core.ui.player.PlayerSnackbarMessageType
import com.flixclusive.core.ui.player.util.PlayerCacheManager
import com.flixclusive.core.ui.player.util.PlayerUiUtil
import com.flixclusive.core.locale.UiText
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.database.WatchTimeUpdaterUseCase
import com.flixclusive.domain.provider.GetMediaLinksUseCase
import com.flixclusive.domain.tmdb.SeasonProviderUseCase
import com.flixclusive.model.film.TvShow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
internal class PlayerScreenViewModel @Inject constructor(
    appSettingsManager: AppSettingsManager,
    client: OkHttpClient,
    context: Context,
    playerCacheManager: PlayerCacheManager,
    savedStateHandle: SavedStateHandle,
    seasonProvider: SeasonProviderUseCase,
    getMediaLinksUseCase: GetMediaLinksUseCase,
    watchHistoryRepository: WatchHistoryRepository,
    watchTimeUpdaterUseCase: WatchTimeUpdaterUseCase,
) : BasePlayerViewModel(
    args = savedStateHandle.navArgs<PlayerScreenNavArgs>(),
    client = client,
    context = context,
    playerCacheManager = playerCacheManager,
    watchHistoryRepository = watchHistoryRepository,
    appSettingsManager = appSettingsManager,
    seasonProviderUseCase = seasonProvider,
    getMediaLinksUseCase = getMediaLinksUseCase,
    watchTimeUpdaterUseCase = watchTimeUpdaterUseCase,
) {
    val snackbarQueue = mutableStateListOf<PlayerSnackbarMessage>()

    // =====================================
    private var snackbarJobs: MutableList<Job?> =
        MutableList(PlayerSnackbarMessageType.entries.size) { null }

    init {
        viewModelScope.launch {
            currentSelectedEpisode.collectLatest {
                if (it?.season != null) {
                    val seasonCount = (film as TvShow).totalSeasons
                    if (it.season != seasonCount) {
                        // Save episodes count of last season
                        // to the watch history item.
                        fetchSeasonFromMetaProvider(seasonNumber = seasonCount)
                    }

                    onSeasonChange(it.season)
                }
            }
        }

        resetUiState()
    }

    override fun showErrorSnackbar(
        message: UiText,
        isInternalPlayerError: Boolean
    ) {
        showSnackbar(
            message = message,
            type = PlayerSnackbarMessageType.Error
        )

        updateWatchHistory(
            currentTime = player.currentPosition,
            duration = player.duration
        )

        if (isInternalPlayerError) {
            selectNextServer()
        }
    }

    fun showSnackbar(message: UiText, type: PlayerSnackbarMessageType) {
        val itemIndexInQueue = snackbarQueue.indexOfFirst {
            it.type == type
        }

        val isErrorType = type == PlayerSnackbarMessageType.Error
        val shouldQueue = itemIndexInQueue == -1
                || isErrorType

        if (shouldQueue) {
            val job = viewModelScope.launch {
                val data = PlayerSnackbarMessage(message, type)
                snackbarQueue.add(data)

                if (type == PlayerSnackbarMessageType.Episode)
                    return@launch

                delay(data.duration.toLong())
                snackbarQueue.remove(data)
            }

            if (isErrorType) {
                var index = PlayerSnackbarMessageType.entries.size
                while (snackbarJobs.getOrNull(index)?.isActive == true && snackbarJobs.getOrNull(index) != null) {
                    index++
                }
                snackbarJobs.add(index, job)
            } else {
                snackbarJobs[type.ordinal] = job
            }

            return
        }

        when (type) {
            PlayerSnackbarMessageType.Episode -> {
                if (snackbarJobs[type.ordinal]?.isActive == true) {
                    snackbarJobs[type.ordinal]?.cancel()
                    snackbarJobs[type.ordinal] = null
                }

                snackbarJobs[type.ordinal] = viewModelScope.launch {
                    val currentIndex =
                        snackbarQueue.indexOfFirst { it.type == type } // Have to call this everytime to be cautious of other snackbar item changes :<

                    val itemInQueue = snackbarQueue[currentIndex]
                    snackbarQueue[currentIndex] = itemInQueue
                        .copy(message = message)

                    delay(itemInQueue.duration.toLong())
                    snackbarQueue.remove(snackbarQueue[currentIndex])
                }
            }
            else -> {
                if (snackbarJobs[type.ordinal]?.isActive == true) {
                    snackbarJobs[type.ordinal]?.cancel()
                    snackbarJobs[type.ordinal] = null
                }

                snackbarQueue.remove(snackbarQueue[itemIndexInQueue])
                showSnackbar(message, type)
            }
        }
    }

    fun removeSnackbar(data: PlayerSnackbarMessage) {
        snackbarQueue.remove(data)
    }

    private fun SnackbarDuration.toLong() = when (this) {
        SnackbarDuration.Short -> 4000L
        SnackbarDuration.Long -> 10000L
        SnackbarDuration.Indefinite -> Long.MAX_VALUE
    }

    private fun selectNextServer() {
        val nextLinkIndex = (uiState.value.selectedSourceLink + 1).takeIf { it <= cachedLinks.streams.lastIndex }

        if (nextLinkIndex != null) {
            val newLink = cachedLinks.streams[nextLinkIndex]
            val currentPlayerTitle = PlayerUiUtil.formatPlayerTitle(film, currentSelectedEpisode.value)

            onServerChange(index = nextLinkIndex)
            player.prepare(
                link = newLink,
                title = currentPlayerTitle,
                subtitles = cachedLinks.subtitles.toList(),
                initialPlaybackPosition = player.currentPosition
            )
        }
    }
}