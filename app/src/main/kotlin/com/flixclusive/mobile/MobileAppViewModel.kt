package com.flixclusive.mobile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.common.provider.MediaLinkResourceState
import com.flixclusive.core.database.entity.toDBFilm
import com.flixclusive.core.database.entity.toWatchlistItem
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.util.awaitFirst
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.monitor.NetworkMonitor
import com.flixclusive.core.ui.mobile.KeyEventHandler
import com.flixclusive.core.util.webview.WebViewDriverManager
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.library.recent.WatchHistoryRepository
import com.flixclusive.data.library.watchlist.WatchlistRepository
import com.flixclusive.data.provider.cache.CacheKey
import com.flixclusive.data.provider.cache.CachedLinksRepository
import com.flixclusive.domain.provider.GetMediaLinksUseCase
import com.flixclusive.domain.session.UserSessionManager
import com.flixclusive.domain.tmdb.usecase.GetFilmMetadataUseCase
import com.flixclusive.model.datastore.user.PlayerPreferences
import com.flixclusive.model.datastore.user.UserPreferences.Companion.PLAYER_PREFS_KEY
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

// TODO: Clean code
@HiltViewModel
internal class MobileAppViewModel
    @Inject
    constructor(
        private val getFilmMetadataUseCase: GetFilmMetadataUseCase,
        private val getMediaLinksUseCase: GetMediaLinksUseCase,
        private val watchHistoryRepository: WatchHistoryRepository,
        private val watchlistRepository: WatchlistRepository,
        private val dataStoreManager: DataStoreManager,
        private val appConfigurationManager: AppConfigurationManager,
        private val userSessionManager: UserSessionManager,
        private val cachedLinksRepository: CachedLinksRepository,
        networkMonitor: com.flixclusive.core.network.util.monitor.NetworkMonitor,
    ) : ViewModel() {
        private var onFilmLongClickJob: Job? = null
        private var onWatchlistClickJob: Job? = null
        private var onRemoveFromWatchHistoryJob: Job? = null
        private var onPlayClickJob: Job? = null

        val keyEventHandlers = mutableListOf<KeyEventHandler>()
        var isInPipMode by mutableStateOf(false)

        val webViewDriver =
            WebViewDriverManager.webView
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        val isConnectedAtNetwork =
            networkMonitor
                .isOnline
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = runBlocking { networkMonitor.isOnline.first() },
                )

        val isPiPModeEnabled: Boolean
            get() =
                dataStoreManager
                    .getUserPrefs<PlayerPreferences>(PLAYER_PREFS_KEY)
                    .awaitFirst()
                    .isPiPModeEnabled

        val currentVersionCode: Long
            get() = appConfigurationManager.currentAppBuild?.build ?: -1

        val hasSeenNewChangelogs =
            with(dataStoreManager.systemPreferences.data) {
                mapLatest { currentVersionCode > it.lastSeenChangelogs }
                    .distinctUntilChanged()
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5000),
                        initialValue = currentVersionCode > awaitFirst().lastSeenChangelogs,
                    )
            }

        private val _uiState = MutableStateFlow(MobileAppUiState())
        val uiState: StateFlow<MobileAppUiState> = _uiState.asStateFlow()

        val episodeToPlay =
            _uiState
                .mapLatest { it.episodeToPlay }
                .distinctUntilChanged()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        val filmToPreview =
            _uiState
                .mapLatest { it.filmToPreview }
                .distinctUntilChanged()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        val loadedCachedLinks =
            combine(
                filmToPreview,
                episodeToPlay,
            ) { film, episode ->
                if (film == null) {
                    null
                } else {
                    film.identifier to episode
                }
            }.filterNotNull()
                .flatMapLatest { (filmId, episode) ->
                    val cacheKey = CacheKey.createFilmOnlyKey(filmId, episode)
                    cachedLinksRepository.observeCache(cacheKey)
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = null,
                )

        fun hideWebViewDriver() {
            WebViewDriverManager.destroy()
        }

        fun previewFilm(film: Film) {
            if (onFilmLongClickJob?.isActive == true) {
                return
            }

            onFilmLongClickJob =
                viewModelScope.launch {
                    val userId = getCurrentSignedInUserId() ?: return@launch

                    val isInWatchlist =
                        watchlistRepository.getWatchlistItemById(
                            itemId = film.identifier,
                            ownerId = userId,
                        ) != null
                    val isInWatchHistory =
                        watchHistoryRepository.getWatchHistoryItemById(
                            itemId = film.identifier,
                            ownerId = userId,
                        ) != null

                    _uiState.update {
                        it.copy(
                            isShowingBottomSheetCard = true,
                            isLongClickedFilmInWatchlist = isInWatchlist,
                            isLongClickedFilmInWatchHistory = isInWatchHistory,
                            filmToPreview = film
                        )
                    }
                }
        }

        fun onBottomSheetClose() {
            _uiState.update {
                it.copy(
                    isShowingBottomSheetCard = false,
                    filmToPreview = null
                )
            }
        }

        fun onWatchlistButtonClick() {
            if (onWatchlistClickJob?.isActive == true) {
                return
            }

            onWatchlistClickJob =
                viewModelScope.launch {
                    filmToPreview.value?.let { film ->
                        val userId = userSessionManager.currentUser.first()?.id ?: return@launch

                        val isInWatchlist = _uiState.value.isLongClickedFilmInWatchlist
                        if (isInWatchlist) {
                            watchlistRepository.removeById(
                                itemId = film.identifier,
                                ownerId = userId,
                            )
                        } else {
                            watchlistRepository.insert(film.toWatchlistItem(userId))
                        }

                        _uiState.update {
                            it.copy(isLongClickedFilmInWatchlist = !isInWatchlist)
                        }
                    }
                }
        }

        fun onRemoveButtonClick() {
            if (onRemoveFromWatchHistoryJob?.isActive == true) {
                return
            }

            onRemoveFromWatchHistoryJob =
                viewModelScope.launch {
                    val userId = getCurrentSignedInUserId() ?: return@launch
                    val isLongClickedFilmInWatchHistory = _uiState.value.isLongClickedFilmInWatchHistory

                    if (isLongClickedFilmInWatchHistory) {
                        filmToPreview.value?.let { film ->
                            _uiState.update {
                                it.copy(isLongClickedFilmInWatchHistory = false)
                            }

                            watchHistoryRepository.deleteById(
                                itemId = film.identifier,
                                ownerId = userId,
                            )
                        }
                    }
                }
        }

        fun onPlayClick(
            film: Film? = null,
            episode: Episode? = null,
        ) {
            if (onPlayClickJob?.isActive == true) {
                return
            }

            onPlayClickJob =
                viewModelScope.launch {
                    updateVideoDataDialogState(MediaLinkResourceState.Fetching(LocaleR.string.film_data_fetching))

                    var filmToShow = film ?: filmToPreview.value ?: return@launch
                    val userId = getCurrentSignedInUserId() ?: return@launch

                    val response =
                        when {
                            filmToShow !is FilmMetadata -> {
                                getFilmMetadataUseCase(partiallyDetailedFilm = filmToShow)
                            }

                            else -> Resource.Success(filmToShow)
                        }

                    val errorFetchingFilm = MediaLinkResourceState.Error(LocaleR.string.film_data_fetch_failed)
                    if (response !is Resource.Success) {
                        return@launch updateVideoDataDialogState(errorFetchingFilm)
                    }

                    filmToShow = response.data
                        ?: return@launch updateVideoDataDialogState(errorFetchingFilm)

                    val watchHistoryItem =
                        watchHistoryRepository
                            .getWatchHistoryItemById(
                                itemId = filmToShow.identifier,
                                ownerId = userId,
                            )?.copy(film = filmToShow.toDBFilm())
                            ?.also { item ->
                                viewModelScope.launch {
                                    watchHistoryRepository.insert(item)
                                }
                            }

                    _uiState.update { it.copy(filmToPreview = filmToShow) }

                    getMediaLinksUseCase(
                        film = filmToShow,
                        watchHistoryItem = watchHistoryItem,
                        episode = episode,
                        onSuccess = { episodeToPlay ->
                            _uiState.update { it.copy(episodeToPlay = episodeToPlay) }
                        },
                    ).collectLatest(::updateVideoDataDialogState)
                }
        }

        private fun updateVideoDataDialogState(mediaLinkResourceState: MediaLinkResourceState) {
            _uiState.update {
                it.copy(mediaLinkResourceState = mediaLinkResourceState)
            }
        }

        fun onConsumeLinkLoaderSheet(isForceClosing: Boolean = false) {
            updateVideoDataDialogState(MediaLinkResourceState.Idle)
            if (isForceClosing) {
                onPlayClickJob?.cancel() // Cancel job
                onPlayClickJob = null
            }

            _uiState.update { it.copy(episodeToPlay = null) }
        }

        fun setPlayerModeState(isInPlayer: Boolean) {
            _uiState.update { it.copy(isOnPlayerScreen = isInPlayer) }
        }

        fun onSaveLastSeenChangelogs(version: Long) {
            viewModelScope.launch {
                dataStoreManager.updateSystemPrefs {
                    it.copy(lastSeenChangelogs = version)
                }
            }
        }

        private fun getCurrentSignedInUserId(): Int? = userSessionManager.currentUser.value?.id
    }
