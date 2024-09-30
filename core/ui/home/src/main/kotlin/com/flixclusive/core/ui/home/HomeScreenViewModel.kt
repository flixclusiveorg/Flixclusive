package com.flixclusive.core.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.core.datastore.AppSettingsManager
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.util.InternetMonitor
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.home.HomeItemsProviderUseCase
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.film.Film
import com.flixclusive.model.provider.Catalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    var itemsSize by mutableIntStateOf(0) // For TV
        private set

    private val paginationJobs = mutableMapOf<Int, Job?>()

    val state = homeItemsProviderUseCase.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = homeItemsProviderUseCase.state.value
        )

    val appSettings = appSettingsManager.appSettings
        .data
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsManager.cachedAppSettings
        )

    private val connectionObserver = internetMonitor
        .isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val continueWatchingList = watchHistoryRepository
        .getAllItemsInFlow()
        .map { items ->
            items.filterNot(::filterWatchedFilms)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = runBlocking {
                watchHistoryRepository.getAllItemsInFlow()
                    .first()
                    .filterNot(::filterWatchedFilms)
            }
        )

    init {
        viewModelScope.launch {
            connectionObserver
                .collectLatest { isConnected ->
                    val status = homeItemsProviderUseCase.state.map {
                        it.status
                    }.first()

                    if (isConnected && status is Resource.Failure || status is Resource.Loading) {
                        initialize()
                    } else if(status is Resource.Success) {
                        onPaginateCatalogs()
                    }
                }
        }
    }

    fun initialize() {
        homeItemsProviderUseCase()
    }

    suspend fun loadFocusedFilm(film: Film) {
        homeItemsProviderUseCase.getFocusedFilm(film)
    }

    fun onPaginateCatalogs() {
        viewModelScope.launch {
            itemsSize += homeItemsProviderUseCase.state.value.catalogs.size
        }
    }

    fun onPaginateFilms(
        catalog: Catalog,
        page: Int,
        index: Int
    ) {
        homeItemsProviderUseCase.state.value.run {
            if(paginationJobs[index]?.isActive == true)
                return

            paginationJobs[index] = viewModelScope.launch {
                homeItemsProviderUseCase.getCatalogItems(
                    catalog = catalog,
                    index = index,
                    page = page
                )
            }
        }
    }
}