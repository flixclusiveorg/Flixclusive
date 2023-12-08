package com.flixclusive.presentation.mobile.screens.search.content

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.model.config.SearchCategoryItem
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.service.network.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.random.Random.Default.nextInt

@HiltViewModel
class SearchInitialContentViewModel @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    val configurationProvider: ConfigurationProvider,
    networkConnectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchInitialContentUiState(isLoading = true))
    val state = _state.asStateFlow()

    private var initializeJob: Job? = null

    private val connectionObserver = networkConnectivityObserver
        .connectivityState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    private var isInitialized = false

    val genres = mutableStateListOf<SearchCategoryItem>()
    val filmTypes = mutableStateListOf<SearchCategoryItem>()
    val networks = configurationProvider.searchCategoriesConfig!!.networks.shuffled()
    val companies = configurationProvider.searchCategoriesConfig!!.companies.shuffled()
    private val usedPosterPaths = mutableMapOf<String, Boolean>()

    init {
        viewModelScope.launch {
            connectionObserver.collect { isConnected ->
                if (isConnected && (_state.value.hasErrors || !isInitialized)) {
                    initialize()
                }
            }
        }
    }

    fun initialize() {
        if (initializeJob?.isActive == true)
            return

        _state.update { SearchInitialContentUiState(isLoading = true) }

        initializeJob = viewModelScope.launch {
            usedPosterPaths.clear()
            // Items that need thumbnails =====
            getFilmTypeThumbnails()
            getGenresThumbnails()
            // ============================ END
            isInitialized = true

            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun getGenresThumbnails() {
        val randomPage = max(1, nextInt(1, 3000) % 5)

        genres.clear()
        genres.addAll(configurationProvider.searchCategoriesConfig!!.genres)

        configurationProvider
            .searchCategoriesConfig!!
            .genres
            .forEachIndexed { i, genre ->
                val pageToUse = if(genre.name.equals("reality", true))
                    1
                else randomPage

                when (
                    val result = tmdbRepository.paginateConfigItems(
                        url = genre.query, page = pageToUse
                    )
                ) {
                    is Resource.Failure -> {
                        _state.update {
                            it.copy(
                                hasErrors = true,
                                isLoading = false
                            )
                        }
                        return
                    }
                    is Resource.Success -> {
                        result.data?.let { data ->
                            var imageToUse: String? = null

                            if(data.results.isEmpty())
                                return@forEachIndexed

                            while (
                                usedPosterPaths[imageToUse] != null
                                || imageToUse == null
                            ) {
                                imageToUse = data.results.random().backdropImage
                            }

                            usedPosterPaths[imageToUse] = true
                            genres[i] = genres[i].copy(
                                posterPath = imageToUse
                            )
                        }
                    }

                    else -> Unit
                }
            }
    }

    private suspend fun getFilmTypeThumbnails() {
        val randomPage = max(1, nextInt(1, 3000) % 5)

        filmTypes.clear()
        filmTypes.addAll(configurationProvider.searchCategoriesConfig!!.type)

        configurationProvider
            .searchCategoriesConfig!!
            .type
            .forEachIndexed { i, type ->
                when (
                    val result = tmdbRepository.paginateConfigItems(
                        url = type.query, page = randomPage
                    )
                ) {
                    is Resource.Failure -> {
                        _state.update {
                            it.copy(
                                hasErrors = true,
                                isLoading = false
                            )
                        }
                        return
                    }
                    is Resource.Success -> {
                        result.data?.let { data ->
                            var imageToUse: String? = null

                            if(data.results.isEmpty())
                                return@forEachIndexed

                            while (
                                usedPosterPaths[imageToUse] != null
                                || imageToUse == null
                            ) {
                                imageToUse = data.results.random().backdropImage
                            }

                            usedPosterPaths[imageToUse] = true
                            filmTypes[i] = filmTypes[i].copy(
                                posterPath = imageToUse
                            )
                        }
                    }
                    else -> Unit
                }
            }
    }
}