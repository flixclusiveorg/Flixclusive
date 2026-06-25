package com.flixclusive.feature.tv.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.strings.UiText
import com.flixclusive.core.ui.media.BaseMediaScreenViewModel
import com.flixclusive.core.ui.media.MediaScreenNavArgs
import com.flixclusive.data.library.recent.WatchHistoryRepository
import com.flixclusive.domain.library.watchlist.ToggleWatchlistStatusUseCase
import com.flixclusive.domain.tmdb.usecase.GetMediaMetadataUseCase
import com.flixclusive.domain.tmdb.usecase.SeasonProviderUseCase
import com.flixclusive.domain.session.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MediaScreenViewModel @Inject constructor(
    mediaProvider: GetMediaMetadataUseCase,
    watchHistoryRepository: WatchHistoryRepository,
    seasonProvider: SeasonProviderUseCase,
    toggleWatchlistStatusUseCase: ToggleWatchlistStatusUseCase,
    savedStateHandle: SavedStateHandle,
    dataStoreManager: DataStoreManager,
    userSessionManager: UserSessionManager,
) : BaseMediaScreenViewModel(
        partiallyDetailedMedia = savedStateHandle.navArgs<MediaScreenNavArgs>().media,
        watchHistoryRepository = watchHistoryRepository,
        seasonProvider = seasonProvider,
        mediaProvider = mediaProvider,
        toggleWatchlistStatusUseCase = toggleWatchlistStatusUseCase,
        dataStoreManager = dataStoreManager,
        userSessionManager = userSessionManager
    ) {
    var errorSnackBarMessage by mutableStateOf<UiText?>(UiText.StringValue("ERR:: 404 ASDkasdmlaskdmasl"))
        private set

    init {
        val errors = uiState
            .map {
                it.errorMessage
            }.distinctUntilChanged()

        viewModelScope.launch {
            errors.collect(::triggerSnackbar)
        }
    }

    private suspend fun triggerSnackbar(error: UiText?) {
        if (errorSnackBarMessage != null) {
            errorSnackBarMessage = null
        }

        errorSnackBarMessage = error
        delay(5000)
        errorSnackBarMessage = null
    }
}
