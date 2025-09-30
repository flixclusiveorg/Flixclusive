package com.flixclusive.mobile

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.BuildConfig
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.exception.ExceptionWithUiText
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.network.monitor.NetworkMonitor
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.webview.WebViewDriverManager
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flixclusive.core.strings.R as LocaleR

@HiltViewModel
internal class MobileAppViewModel
    @Inject
    constructor(
        private val _getFilmMetadata: GetFilmMetadataUseCase,
        private val getSeasonWithWatchProgress: GetSeasonWithWatchProgressUseCase,
        private val getMediaLinks: GetMediaLinksUseCase,
        private val watchProgressRepository: WatchProgressRepository,
        private val watchlistRepository: WatchlistRepository,
        private val dataStoreManager: DataStoreManager,
        private val userSessionManager: UserSessionManager,
        private val libraryListRepository: LibraryListRepository,
        private val appDispatchers: AppDispatchers,
        cachedLinksRepository: CachedLinksRepository,
        networkMonitor: NetworkMonitor,
    ) : ViewModel() {
        private var onFilmLongClickJob: Job? = null
        private var onFetchMediaLinksJob: Job? = null

        private val userId: Int
            get() {
                return userSessionManager.currentUser.value?.id
                    ?: error("It is now allowed to browse the app without a logged in user!")
            }

        /**
         * A WebView driver instance that is shared across the app.
         *
         * This is initialized by providers that require a WebView to fetch media links.
         * It is destroyed when the user leaves the app or when it's no longer needed to free up resources.
         * */
        val webViewDriver = WebViewDriverManager.webView
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )

        /**
         * A StateFlow to check if user is connected to the internet.
         * */
        val hasInternet = networkMonitor.isOnline
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = true,
            )

        val hasNotSeenNewChangelogs = dataStoreManager
            .getSystemPrefs()
            .mapLatest { BuildConfig.VERSION_CODE > it.lastSeenChangelogs }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = true,
            )

        private val _uiState = MutableStateFlow(MobileAppUiState())
        val uiState: StateFlow<MobileAppUiState> = _uiState.asStateFlow()

        val currentLinksCache = cachedLinksRepository.currentCache

        fun previewFilm(film: Film) {
            if (onFilmLongClickJob?.isActive == true) return

            onFilmLongClickJob = viewModelScope.launch {
                val watchlistItem = watchlistRepository.get(filmId = film.identifier, ownerId = userId)
                val libraryItem =
                    libraryListRepository.getListsContainingFilm(filmId = film.identifier, ownerId = userId).first()
                val watchProgressItem = watchProgressRepository.get(
                    id = film.identifier,
                    ownerId = userId,
                    type = film.filmType,
                )

                val isInLibrary = libraryItem.isNotEmpty() ||
                    watchProgressItem != null ||
                    watchlistItem != null

                _uiState.update {
                    it.copy(
                        filmPreviewState = FilmPreview(
                            film = film,
                            isInLibrary = isInLibrary,
                        ),
                    )
                }
            }
        }

        fun onFetchMediaLinks(
            film: Film,
            episode: Episode? = null,
        ) {
            if (onFetchMediaLinksJob?.isActive == true) return

            onFetchMediaLinksJob = viewModelScope.launch {
                updateLoadLinksState(LoadLinksState.Fetching(LocaleR.string.film_data_fetching))

                val metadata = getFilmMetadata(film = film)
                if (metadata == null) {
                    updateLoadLinksState(LoadLinksState.Error(LocaleR.string.film_data_fetch_failed))
                    return@launch
                }

                // Data to be passed to the player screen
                var playerData = PlayerData(film = metadata)

                val responseFlow = when (metadata) {
                    is Movie -> getMediaLinks(movie = metadata)
                    is TvShow -> {
                        var episodeToLoad = episode

                        if (episode == null) {
                            getSeason(tvShow = metadata)
                                .onSuccess { episodeToLoad = it }
                                .onFailure { e ->
                                    updateLoadLinksState(LoadLinksState.Error(e))
                                    return@launch
                                }
                        }

                        playerData = playerData.copy(episode = episodeToLoad)

                        getMediaLinks(
                            tvShow = metadata,
                            episode = episodeToLoad!!,
                        )
                    }

                    else -> error("This is not a valid FilmMetadata subclass: $metadata")
                }

                _uiState.update { it.copy(playerData = playerData) }
                responseFlow.collect(::updateLoadLinksState)
            }
        }

        /**
         * Gets a detailed metadata of a non-detailed [Film].
         *
         * This assumes that [Film] could be a search item.
         * */
        private suspend fun getFilmMetadata(film: Film): FilmMetadata? {
            if (film is FilmMetadata) return film

            return when (val response = _getFilmMetadata(film = film)) {
                is Resource.Success -> response.data
                else -> null
            }
        }

        private suspend fun getSeason(tvShow: TvShow): Result<Episode> {
            val episodeProgress = watchProgressRepository.get(
                id = tvShow.identifier,
                ownerId = userId,
                type = tvShow.filmType,
            ) as? EpisodeProgressWithMetadata

            // Default to 1 if this has not been saved yet
            val seasonNumber = episodeProgress?.watchData?.seasonNumber ?: 1
            val episodeNumber = episodeProgress?.watchData?.episodeNumber ?: 1

            val response = getSeasonWithWatchProgress(tvShow = tvShow, number = seasonNumber)
                .dropWhile { it is Resource.Loading }
                .first()

            when (response) {
                is Resource.Failure -> {
                    return Result.failure(ExceptionWithUiText(response.error))
                }

                is Resource.Success -> {
                    val season = response.data!!
                    val episodeWithProgress = season.episodes.fastFirstOrNull { it.number == episodeNumber }

                    if (episodeWithProgress == null) {
                        return Result.failure(ExceptionWithUiText(UiText.from(LocaleR.string.unavailable_episode)))
                    }

                    return Result.success(episodeWithProgress.episode)
                }

                else -> error("Fetching seasons for load links state is still loading!")
            }
        }

        fun hideWebViewDriver() {
            WebViewDriverManager.destroy()
        }

        fun onStopLoadingLinks(isForceClosing: Boolean = false) {
            updateLoadLinksState(LoadLinksState.Idle)
            if (isForceClosing) {
                onFetchMediaLinksJob?.cancel() // Cancel job
            }
        }

        fun onSaveLastSeenChangelogs(version: Long) {
            appDispatchers.ioScope.launch {
                dataStoreManager.updateSystemPrefs {
                    it.copy(lastSeenChangelogs = version)
                }
            }
        }

        fun onRemovePreviewFilm() {
            _uiState.update { it.copy(filmPreviewState = null) }
        }

        fun updateLoadLinksState(state: LoadLinksState) {
            _uiState.update { it.copy(loadLinksState = state) }
        }
    }

@Stable
internal data class MobileAppUiState(
    val loadLinksState: LoadLinksState = LoadLinksState.Idle,
    val filmPreviewState: FilmPreview? = null,
    val playerData: PlayerData? = null,
)

@Stable
internal data class FilmPreview(
    val film: Film,
    val isInLibrary: Boolean,
)

@Stable
internal data class PlayerData(
    val film: Film,
    val episode: Episode? = null,
)
