package com.flixclusive.feature.mobile.media.modal.stream

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefsAsFlow
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.domain.provider.usecase.links.TestLinksProgress
import com.flixclusive.domain.provider.usecase.links.TestMediaLinksUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.ramcosta.composedestinations.generated.media.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.stateIn
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
    private val testMediaLinksUseCase: TestMediaLinksUseCase,
    private val userSessionDataStore: UserSessionDataStore,
    private val watchProgressRepository: WatchProgressRepository,
    private val dataStoreManager: DataStoreManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = savedStateHandle.navArgs<MediaLinksBottomSheetArgs>()

    private var onFetchMediaLinksJob: Job? = null
    private var onRefetchLinks: Job? = null
    private var onTestLinksJob: Job? = null

    private val _uiState = MutableStateFlow(
        MediaLinksBottomSheetUiState(
            metadata = args.media,
            episode = args.episode,
        )
    )
    val uiState = _uiState.asStateFlow()

    val playerPrefs = dataStoreManager
        .getUserPrefsAsFlow<PlayerPreferences>(UserPreferences.PLAYER_PREFS_KEY)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerPreferences(),
        )


    val links = userSessionDataStore.currentUserId
        .filterNotNull()
        .flatMapLatest { ownerId ->
            mediaLinksRepository.observeLinks(
                ownerId = ownerId,
                mediaId = args.media.id,
                episodeNumber = args.episode?.number,
                seasonNumber = args.episode?.season,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val testProgress: MutableStateFlow<TestLinksProgress?> = MutableStateFlow(null)

    init {
        viewModelScope.launch {
            onFetchMediaLinks()
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

    fun onFetchMediaLinks() {
        if (onFetchMediaLinksJob?.isActive == true) return

        onFetchMediaLinksJob?.cancel()
        onFetchMediaLinksJob = viewModelScope.launch {
            updateLoadLinksState(LoadLinksState.Fetching(LocaleR.string.media_data_fetching))

            var metadata = args.media

            if (metadata is PartialMedia) {
                metadata = getMediaMetadata(media = metadata).last()
                    .let {
                        when (it) {
                            is Async.Success -> it.data
                            else -> {
                                updateLoadLinksState(LoadLinksState.Error(LocaleR.string.media_data_fetch_failed))
                                return@launch
                            }
                        }
                    }
            }

            _uiState.update { it.copy(metadata = metadata) }

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
        }
    }

    fun onResetAndRetry() {
        if (onRefetchLinks?.isActive == true || onFetchMediaLinksJob?.isActive == true) return

        onRefetchLinks = viewModelScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val data = mediaLinksRepository.getLinks(
                ownerId = userId,
                mediaId = args.media.id,
                episodeNumber = args.episode?.number,
                seasonNumber = args.episode?.season,
            )

            if (data != null) {
                mediaLinksRepository.deleteAll(data.id)
            }

            onFetchMediaLinks()
        }
    }

    fun onTestLinks() {
        if (onTestLinksJob?.isActive == true) return

        onTestLinksJob = viewModelScope.launch {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val data = mediaLinksRepository.getLinks(
                ownerId = userId,
                mediaId = args.media.id,
                episodeNumber = args.episode?.number,
                seasonNumber = args.episode?.season,
            ) ?: return@launch

            testMediaLinksUseCase(data.id).collect { progress ->
                testProgress.value = progress
            }
        }
    }

    fun updateLoadLinksState(state: LoadLinksState) {
        _uiState.update { it.copy(loadLinksState = state) }
    }
}

@Stable
internal data class MediaLinksBottomSheetUiState(
    val metadata: MediaMetadata,
    val episode: Episode?,
    val loadLinksState: LoadLinksState = LoadLinksState.Idle,
)
