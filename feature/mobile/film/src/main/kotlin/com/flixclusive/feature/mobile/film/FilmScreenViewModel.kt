package com.flixclusive.feature.mobile.film

import androidx.lifecycle.SavedStateHandle
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.ui.film.BaseFilmScreenViewModel
import com.flixclusive.core.ui.film.FilmScreenNavArgs
import com.flixclusive.data.library.recent.WatchHistoryRepository
import com.flixclusive.domain.library.watchlist.ToggleWatchlistStatusUseCase
import com.flixclusive.domain.session.UserSessionManager
import com.flixclusive.domain.tmdb.usecase.GetFilmMetadataUseCase
import com.flixclusive.domain.tmdb.usecase.SeasonProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class FilmScreenViewModel @Inject constructor(
    watchHistoryRepository: WatchHistoryRepository,
    seasonProvider: SeasonProviderUseCase,
    filmProvider: GetFilmMetadataUseCase,
    toggleWatchlistStatusUseCase: ToggleWatchlistStatusUseCase,
    savedStateHandle: SavedStateHandle,
    dataStoreManager: DataStoreManager,
    userSessionManager: UserSessionManager,
) : BaseFilmScreenViewModel(
    partiallyDetailedFilm = savedStateHandle.navArgs<FilmScreenNavArgs>().film,
    watchHistoryRepository = watchHistoryRepository,
    seasonProvider = seasonProvider,
    filmProvider = filmProvider,
    toggleWatchlistStatusUseCase = toggleWatchlistStatusUseCase,
    dataStoreManager = dataStoreManager,
    userSessionManager = userSessionManager
)
