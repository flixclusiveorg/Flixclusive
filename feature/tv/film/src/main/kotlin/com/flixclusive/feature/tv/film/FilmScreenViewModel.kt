package com.flixclusive.feature.tv.film

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.ui.film.BaseFilmScreenViewModel
import com.flixclusive.core.ui.film.FilmScreenNavArgs
import com.flixclusive.core.locale.UiText
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.database.ToggleWatchlistStatusUseCase
import com.flixclusive.domain.tmdb.FilmProviderUseCase
import com.flixclusive.domain.tmdb.SeasonProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class FilmScreenViewModel @Inject constructor(
    filmProvider: FilmProviderUseCase,
    watchHistoryRepository: WatchHistoryRepository,
    seasonProvider: SeasonProviderUseCase,
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
) {
    var errorSnackBarMessage by mutableStateOf<UiText?>(UiText.StringValue("ERR:: 404 ASDkasdmlaskdmasl"))
        private set

    init {
        val errors = uiState.map {
            it.errorMessage
        }.distinctUntilChanged()

        viewModelScope.launch {
            errors.collect(::triggerSnackbar)
        }
    }

    private suspend fun triggerSnackbar(error: UiText?) {
        if(errorSnackBarMessage != null)
            errorSnackBarMessage = null

        errorSnackBarMessage = error
        delay(5000)
        errorSnackBarMessage = null
    }
}