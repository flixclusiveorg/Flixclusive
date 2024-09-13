package com.flixclusive.feature.mobile.film

import androidx.lifecycle.SavedStateHandle
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.ui.film.BaseFilmScreenViewModel
import com.flixclusive.core.ui.film.FilmScreenNavArgs
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.database.ToggleWatchlistStatusUseCase
import com.flixclusive.domain.tmdb.FilmProviderUseCase
import com.flixclusive.domain.tmdb.SeasonProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class FilmScreenViewModel @Inject constructor(
    watchHistoryRepository: WatchHistoryRepository,
    seasonProvider: SeasonProviderUseCase,
    filmProvider: FilmProviderUseCase,
    toggleWatchlistStatusUseCase: ToggleWatchlistStatusUseCase,
    savedStateHandle: SavedStateHandle,
    appSettingsManager: AppSettingsManager
) : BaseFilmScreenViewModel(
    partiallyDetailedFilm = savedStateHandle.navArgs<FilmScreenNavArgs>().film,
    watchHistoryRepository = watchHistoryRepository,
    seasonProvider = seasonProvider,
    filmProvider = filmProvider,
    toggleWatchlistStatusUseCase = toggleWatchlistStatusUseCase,
    appSettingsManager = appSettingsManager
)