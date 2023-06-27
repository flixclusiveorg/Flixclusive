package com.flixclusive.presentation.home.content

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.usecase.HomeItemConfig
import com.flixclusive.domain.usecase.HomeItemsProviderUseCase
import com.flixclusive.presentation.common.network.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class HomeContentViewModel @Inject constructor(
    watchHistoryRepository: WatchHistoryRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
    private val homeItemsProvider: HomeItemsProviderUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val homeRowItems = mutableStateListOf<HomeItemConfig>()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val connectionObserver = networkConnectivityObserver
        .connectivityState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private var isInitialized = false

    val continueWatchingList = watchHistoryRepository
        .getAllItemsInFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    init {
        viewModelScope.launch {
            connectionObserver.collect { isConnected ->
                val hasErrors = _uiState.value.hasErrors

                if(isConnected && hasErrors || !isInitialized) {
                    initialize()
                }
            }
        }
    }

    fun initialize() {
        _uiState.update { HomeUiState() }
        homeRowItems.clear()

        loadTrendingAndPopularLists()
        isInitialized = true
    }

    private fun loadTrendingAndPopularLists() {
        viewModelScope.launch(ioDispatcher) {
            val headerItem = homeItemsProvider.getHeaderItem()

            _uiState.update {
                it.copy(
                    headerItem = headerItem,
                    hasErrors = headerItem == null,
                )
            }

            if(headerItem == null)
                return@launch

            val mainRowItems = homeItemsProvider.getMainRowItems()
            val watchProviderRowItems = homeItemsProvider.getWatchProvidersRowItems(count = Random.nextInt(1, 5))
            val genreRowItems = homeItemsProvider.getGenreRowItems(count = Random.nextInt(1, 6))
            val basedOnRowItems = homeItemsProvider.getBasedOnRowItems(count = Random.nextInt(1, 4))

            merge(
                mainRowItems,
                watchProviderRowItems,
                genreRowItems,
                basedOnRowItems
            ).collect { item ->
                if(item == null) {
                    _uiState.update {
                        it.copy(hasErrors = true)
                    }
                    cancel()
                    return@collect
                }

                homeRowItems.add(item)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}