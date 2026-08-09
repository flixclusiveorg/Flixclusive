package com.flixclusive.feature.mobile.media.modal.stream

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastFlatMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.provider.CachedMediaLink
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.DataStoreManager.Companion.getUserPrefsAsFlow
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.data.downloads.repository.MediaDownloadRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.downloads.usecase.GetCompletedDownloadFileUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetNextEpisodeUseCase
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import com.ramcosta.composedestinations.generated.media.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class MediaLinksBottomSheetViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getMediaLinks: GetMediaLinksUseCase,
    private val getMediaMetadata: GetMediaMetadataUseCase,
    private val getNextEpisode: GetNextEpisodeUseCase,
    private val getCompletedDownloadFile: GetCompletedDownloadFileUseCase,
    private val mediaDownloadRepository: MediaDownloadRepository,
    private val mediaLinksRepository: MediaLinksRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val watchProgressRepository: WatchProgressRepository,
    private val providerRepository: ProviderRepository,
    private val appDispatchers: AppDispatchers,
    dataStoreManager: DataStoreManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = savedStateHandle.navArgs<MediaLinksBottomSheetArgs>()

    private var onFetchMediaLinksJob: Job? = null
    private var onRefetchLinks: Job? = null

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

    /** Only what providers resolved. Kept apart from [links] because "is there anything playable
     * yet" and "may the loading screen be skipped" are questions about the providers alone — a
     * downloaded file answering them would offer to skip ahead to a provider stream that does not
     * exist. */
    val providerLinks = combine(
        userSessionDataStore.currentUserId.filterNotNull(),
        _uiState.map { it.episode }.distinctUntilChanged()
    ) { userId, episode -> userId to episode }
        // If it's a show, wait until we have the episode to load the links
        .dropWhile { args.media.isShow && it.second == null }
        .flatMapLatest { (userId, episode) ->
            mediaLinksRepository
                .observeLinks(
                    ownerId = userId,
                    mediaId = args.media.id,
                    episodeNumber = episode?.number,
                    seasonNumber = episode?.season,
                ).mapLatest { data ->
                    data.fastFlatMap { it.streams + it.subtitles }
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    /**
     * The finished download for what is about to be watched, if there is one.
     *
     * Resolved from the session and the database alone — never from [_uiState], whose episode is
     * only filled in after [onFetchMediaLinks] has been to the network. That independence is what
     * lets a download play with no connection at all.
     */
    val localLink = userSessionDataStore.currentUserId
        .filterNotNull()
        .mapLatest { userId -> resolveLocalLink(userId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    /** What the sheet lists: the downloaded file first, then everything the providers found. */
    val links: StateFlow<List<CachedMediaLink>> = combine(
        localLink,
        providerLinks,
    ) { local, provider -> listOfNotNull(local?.stream) + provider }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private suspend fun resolveLocalLink(userId: String): LocalLink? {
        val progress = if (args.media.isShow) {
            watchProgressRepository.get(
                id = args.media.id,
                ownerId = userId,
                type = args.media.type,
            ) as? EpisodeProgressWithMetadata
        } else {
            null
        }

        val targets = localPlaybackTargets(
            isShow = args.media.isShow,
            navEpisode = args.episode,
            progressSeason = progress?.watchData?.seasonNumber,
            progressEpisode = progress?.watchData?.episodeNumber,
            isProgressCompleted = progress?.watchData?.isCompleted == true,
        )

        for (target in targets) {
            val item = mediaDownloadRepository.getCompletedFor(
                mediaId = args.media.id,
                seasonNumber = target.seasonNumber,
                episodeNumber = target.episodeNumber,
            ) ?: continue

            // A row can outlive its file when the user clears it from outside the app. Resolving it
            // here is the same precondition DownloadsTweakViewModel.onOpen checks before opening the
            // player, so a vanished file means no link rather than one that dead-ends.
            val file = getCompletedDownloadFile(item) ?: continue

            return LocalLink(
                downloadItemId = item.id,
                stream = item.toLocalCachedStream(
                    file = file,
                    ownerId = userId,
                    label = context.getString(LocaleR.string.label_downloaded_link),
                ),
            )
        }

        return null
    }

    init {
        onFetchMediaLinks()
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

        var season = tvShow.seasons.getOrNull(seasonIndex)
        if (season is Season.Partial) {
            val provider = providerRepository.getProvider(
                id = tvShow.providerId,
                ownerId = userId
            )

            val api = provider?.plugin?.getMetadataApi(context)
            if (api == null || !provider.isMetadataEnabled) {
                return null
            }

            season = api.getSeason(
                show = tvShow,
                season = season,
            )
        }

        if (season !is Season.Full) {
            warnLog("Season $seasonNumber not found for show ${tvShow.title} (${tvShow.id})")
            return null
        }

        val episode = season.episodes
            .binarySearch {
                it.number.compareTo(episodeNumber)
            }.let { index -> season.episodes.getOrNull(index) }

        return episode
    }

    fun onFetchMediaLinks() {
        onFetchMediaLinksJob?.cancel()
        onFetchMediaLinksJob = viewModelScope.launch {
            updateLoadLinksState(LoadLinksState.Fetching(LocaleR.string.media_data_fetching))

            var metadata = args.media

            if (metadata is PartialMedia) {
                metadata = getMediaMetadata(media = metadata)
                    .last()
                    .let {
                        when (it) {
                            is Async.Success -> {
                                it.data
                            }

                            is Async.Failure -> {
                                updateLoadLinksState(LoadLinksState.Error(it.message))
                                return@launch
                            }

                            else -> {
                                updateLoadLinksState(LoadLinksState.Error(LocaleR.string.media_data_fetch_failed))
                                return@launch
                            }
                        }
                    }
            }

            if (metadata is PartialMedia) {
                updateLoadLinksState(LoadLinksState.Error(LocaleR.string.media_data_fetch_failed))
                return@launch
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

        onRefetchLinks = appDispatchers.ioScope
            .launch {
                val userId = userSessionDataStore.currentUserId.filterNotNull().first()
                mediaLinksRepository.deleteLinks(
                    ownerId = userId,
                    mediaId = args.media.id,
                    episodeNumber = args.episode?.number,
                    seasonNumber = args.episode?.season,
                )
            }.also {
                it.invokeOnCompletion { onFetchMediaLinks() }
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
