package com.flixclusive.presentation.common.viewmodels.home

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.common.LoggerUtils.errorLog
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.model.config.HomeCategoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.usecase.HomeItemsProviderUseCase
import com.flixclusive.presentation.common.NetworkConnectivityObserver
import com.flixclusive.presentation.common.PagingState
import com.flixclusive.presentation.navArgs
import com.flixclusive.presentation.tv.common.DefaultTvNavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

const val HOME_MAX_PAGE = 5

data class PaginationStateInfo(
    val canPaginate: Boolean,
    val pagingState: PagingState,
    val currentPage: Int,
)

@HiltViewModel
class HomeContentScreenViewModel @Inject constructor(
    watchHistoryRepository: WatchHistoryRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
    private val homeItemsProvider: HomeItemsProviderUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val isOnTvScreen = savedStateHandle.navArgs<DefaultTvNavArgs>().isOnTvScreen

    val homeCategories = mutableStateListOf<HomeCategoryItem>()
    val homeRowItems = mutableStateListOf<SnapshotStateList<Film>>()
    val homeRowItemsPagingState = mutableStateListOf<PaginationStateInfo>()
    private val homeRowItemsPaginationJobs = mutableStateListOf<Job?>()

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
        homeCategories.clear()
        homeRowItems.clear()
        homeRowItemsPagingState.clear()
        homeRowItemsPaginationJobs.clear()

        loadTrendingAndPopularLists()
        isInitialized = true
    }

    fun loadFocusedFilm(film: Film) {
        viewModelScope.launch {
            val headerItem = homeItemsProvider.getFocusedItem(film)
            _uiState.update {
                it.copy(
                    headerItem = headerItem
                )
            }
        }
    }

    private fun loadTrendingAndPopularLists() {
        viewModelScope.launch {
            if(isOnTvScreen == false || isOnTvScreen == null) {
                val headerItem = homeItemsProvider.getHeaderItem()

                _uiState.update {
                    it.copy(
                        headerItem = headerItem,
                        hasErrors = headerItem == null,
                    )
                }

                if(headerItem == null)
                    return@launch
            }

            val basedOnRowItems = homeItemsProvider.getUserRecommendations(count = Random.nextInt(1, 4))
            val homeItems = homeItemsProvider.getHomeRecommendations()

            merge(
                homeItems,
                basedOnRowItems
            ).onEach { item ->
                if(item == null) {
                    _uiState.update {
                        it.copy(hasErrors = true)
                    }
                    cancel()
                    return@onEach
                }


                homeCategories.add(item)
                homeRowItems.add(mutableStateListOf())
                homeRowItemsPagingState.add(
                    PaginationStateInfo(
                        canPaginate = item.canPaginate,
                        pagingState = if (!item.canPaginate) PagingState.NON_PAGEABLE else PagingState.IDLE,
                        currentPage = 1
                    )
                )
                homeRowItemsPaginationJobs.add(null)
            }.onCompletion { e ->
                if(e == null) {
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    errorLog(e.stackTraceToString())
                    _uiState.update { it.copy(hasErrors = true) }
                }
            }.collect()
        }
    }

    fun onLastItemFocusChange(row: Int, column: Int) {
        _uiState.update {
            it.copy(lastFocusedItem = FocusPosition(row, column))
        }
    }

    fun onPaginate(query: String, page: Int, index: Int) {
        if(homeRowItemsPaginationJobs[index]?.isActive == true)
            return

        homeRowItemsPaginationJobs[index] = viewModelScope.launch(ioDispatcher) {
            homeItemsProvider.getHomeItems(
                query = query,
                page = page,
                onFailure = {
                    homeRowItemsPagingState[index] = homeRowItemsPagingState[index].copy(
                        pagingState = when(page) {
                            1 -> PagingState.ERROR
                            else -> PagingState.PAGINATING_EXHAUST
                        }
                    )
                },
                onSuccess = { data ->
                    val maxPage = if(data.totalPages >= HOME_MAX_PAGE) HOME_MAX_PAGE else data.totalPages
                    val canPaginate = data.results.size == 20 && page < maxPage

                    if(page == 1) {
                        homeRowItems[index].clear()
                    }

                    homeRowItems[index].addAll(
                        data.results.filter { item ->
                            !homeRowItems[index].contains(item)
                        }
                    )

                    homeRowItemsPagingState[index] = homeRowItemsPagingState[index].copy(
                        canPaginate = canPaginate,
                        pagingState = PagingState.IDLE,
                        currentPage = if(canPaginate) page + 1 else page
                    )
                }
            )
        }
    }
}