package com.flixclusive.core.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.data.util.InternetMonitor
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.home.HomeItemsProviderUseCase
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.tmdb.Film
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


private fun filterWatchedFilms(watchHistoryItem: WatchHistoryItem): Boolean {
    val isTvShow = watchHistoryItem.seasons != null

    var isFinished = true
    if (watchHistoryItem.episodesWatched.isEmpty()) {
        isFinished = false
    } else if(isTvShow) {
        val (nextEpisodeToWatch, _) = getNextEpisodeToWatch(watchHistoryItem)
        if(nextEpisodeToWatch != null)
            isFinished = false
    } else {
        isFinished = watchHistoryItem.episodesWatched.last().isFinished
    }

    return isFinished
}

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val homeItemsProviderUseCase: HomeItemsProviderUseCase,
    appSettingsManager: AppSettingsManager,
    internetMonitor: InternetMonitor,
    watchHistoryRepository: WatchHistoryRepository,
) : ViewModel() {
    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.localAppSettings
        )

    var itemsSize by mutableIntStateOf(0) // For TV
        private set


    val headerItem = homeItemsProviderUseCase.headerItem
    val homeCategories = homeItemsProviderUseCase.categories
    val homeRowItems = homeItemsProviderUseCase.rowItems
    val homeRowItemsPagingState = homeItemsProviderUseCase.rowItemsPagingState

    val uiState = homeItemsProviderUseCase.initializationStatus

    private val connectionObserver = internetMonitor
        .isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val continueWatchingList = watchHistoryRepository
        .getAllItemsInFlow()
        .onEach { items ->
            items.filterNot(::filterWatchedFilms)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking { watchHistoryRepository.getAllItemsInFlow().first()  }
        )

    init {
        viewModelScope.launch {
            connectionObserver
                .combine(homeItemsProviderUseCase.initializationStatus) { isConnected, status ->
                    isConnected to status
                }
                .onEach { (isConnected, status) ->
                    if (isConnected && status is Resource.Failure || status is Resource.Loading) {
                        initialize()
                    } else if(status is Resource.Success) {
                        onPaginateCategories()
                    }
                }
                .collect()
        }
    }

    fun initialize() {
        homeItemsProviderUseCase()
    }

    suspend fun loadFocusedFilm(film: Film) {
        homeItemsProviderUseCase.getFocusedFilm(film)
    }

    fun onPaginateCategories() {
        viewModelScope.launch {
            itemsSize += homeCategories.first().size
        }
    }

    fun onPaginateFilms(
        query: String,
        page: Int,
        index: Int
    ) {
        homeItemsProviderUseCase.run {
            if(rowItemsPaginationJobs[index]?.isActive == true)
                return

            rowItemsPaginationJobs[index] = viewModelScope.launch {
                homeItemsProviderUseCase.getHomeItems(
                    query = query,
                    index = index,
                    page = page,
                )
            }
        }
    }
}