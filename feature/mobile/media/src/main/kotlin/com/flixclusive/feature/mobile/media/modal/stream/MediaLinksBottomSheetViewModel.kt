package com.flixclusive.feature.mobile.media.modal.stream

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.provider.repository.MediaLinksCacheKey.Companion.toCacheKey
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.ramcosta.composedestinations.generated.media.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class MediaLinksBottomSheetViewModel @Inject constructor(
    private val getMediaLinks: GetMediaLinksUseCase,
    private val getMediaMetadata: GetMediaMetadataUseCase,
    private val getNextEpisode: GetNextEpisodeUseCase,
    private val mediaLinksRepository: MediaLinksRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val watchProgressRepository: WatchProgressRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = savedStateHandle.navArgs<MediaLinksBottomSheetArgs>()

    private var onFetchMediaLinksJob: Job? = null

    private val _uiState = MutableStateFlow(
        MediaLinksBottomSheetUiState(
            metadata = args.media,
            episode = args.episode,
        )
    )
    val uiState = _uiState.asStateFlow()

    val currentObservableLinks = mediaLinksRepository.currentObservable

    init {
        onFetchMediaLinks()
    }

    fun onFetchMediaLinks() {
        if (onFetchMediaLinksJob?.isActive == true) return

        onFetchMediaLinksJob?.cancel()
        onFetchMediaLinksJob = viewModelScope.launch {
            mediaLinksRepository.setCurrentObservable(null)
            updateLoadLinksState(LoadLinksState.Fetching(LocaleR.string.media_data_fetching))

            val media = args.media
            val metadata = if (args.media is PartialMedia) {
                getMediaMetadata(media = media).last().let {
                    when (it) {
                        is Async.Success -> it.data
                        else -> {
                            updateLoadLinksState(LoadLinksState.Error(LocaleR.string.media_data_fetch_failed))
                            return@launch
                        }
                    }
                }
            } else {
                media
            }

            _uiState.update { it.copy(metadata = metadata) }

            // Data to be passed to the player screen
            var episodeToLoad = args.episode
            if (metadata is Show) {
                if (args.episode == null) {
                    episodeToLoad = getEpisodeToWatch(tvShow = metadata)
                }

                if (episodeToLoad == null) {
                    updateLoadLinksState(LoadLinksState.Error(LocaleR.string.failed_to_load_episode))
                    return@launch
                }

                _uiState.update { it.copy(episode = episodeToLoad) }
            }

            val response = getMediaLinks(
                media = metadata,
                episode = episodeToLoad,
            )

            response.collect(::updateLoadLinksState)

            if (isFailureButHasLinks()) {
                mediaLinksRepository.setCurrentObservable(null)
            }
        }
    }

    private suspend fun getEpisodeToWatch(tvShow: Show): Episode? {
        val userId = userSessionDataStore.currentUserId.filterNotNull().first()
        val progress = watchProgressRepository.get(
            id = tvShow.id,
            ownerId = userId,
            type = tvShow.type,
        ) as? EpisodeProgressWithMetadata

        if (progress?.watchData?.isCompleted == true) {
            return getNextEpisode(
                show = tvShow,
                season = progress.watchData.seasonNumber,
                episode = progress.watchData.episodeNumber,
            )
        }

        // Default to 1 if this has not been saved yet
        val seasonNumber = progress?.watchData?.seasonNumber ?: 1
        val episodeNumber = progress?.watchData?.episodeNumber ?: 1


        val seasonIndex = tvShow.seasons.binarySearch {
            it.number.compareTo(seasonNumber)
        }

        val season = tvShow.seasons.getOrNull(seasonIndex)

        val episode = season?.episodes?.binarySearch {
            it.number.compareTo(episodeNumber)
        }?.let { index -> season.episodes.getOrNull(index) }

        return episode
    }

    private fun isFailureButHasLinks(): Boolean {
        val currentCache = currentObservableLinks.value
        val loadLinksState = _uiState.value.loadLinksState

        return loadLinksState.isError
            && currentCache != null
            && currentCache.hasStreamableLinks
    }

    fun updateLoadLinksState(state: LoadLinksState) {
        if (state.hasProviderId) {
            val (media, episode) = _uiState.value
            val cache = state.toCacheKey(
                mediaId = media.id,
                episode = episode,
            )

            if (cache != null) {
                mediaLinksRepository.setCurrentObservable(cache)
            }
        }

        _uiState.update { it.copy(loadLinksState = state) }
    }
}

@Stable
internal data class MediaLinksBottomSheetUiState(
    val metadata: MediaMetadata,
    val episode: Episode?,
    val loadLinksState: LoadLinksState = LoadLinksState.Idle,
)
