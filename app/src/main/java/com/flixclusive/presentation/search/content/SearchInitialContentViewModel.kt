package com.flixclusive.presentation.search.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixclusive.di.DefaultDispatcher
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.domain.repository.SortOptions
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.presentation.common.network.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchInitialContentViewModel @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
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

    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()

    init {
        viewModelScope.launch(defaultDispatcher) {
            connectionObserver.collect { isConnected ->
                if(isConnected && _state.value.hasErrors || !isInitialized) {
                    initialize()
                }
            }
        }
    }

    fun initialize() {
        if(initializeJob?.isActive == true)
            return

        initializeJob = viewModelScope.launch {
            _state.update { SearchInitialContentUiState(isLoading = true) }
            _genres.update { emptyList() }

            loadGenres()
            getGenreThumbnails()
            isInitialized = true
        }
    }

    private suspend fun loadGenres() {
        var list: List<Genre> = emptyList()

        when(val movieGenres = tmdbRepository.getGenres(mediaType = "movie")) {
            is Resource.Failure -> {
                _state.update {
                    it.copy(
                        hasErrors = true,
                        isLoading = false
                    )
                }
                return
            }
            Resource.Loading -> Unit
            is Resource.Success -> {
                movieGenres.data?.let { genresList ->
                    list = genresList.map { it.copy(mediaType = "movie") }
                }
            }
        }

        when(val tvGenres = tmdbRepository.getGenres(mediaType = "tv")) {
            is Resource.Failure -> {
                _state.update {
                    it.copy(
                        hasErrors = true,
                        isLoading = false
                    )
                }
                return
            }
            Resource.Loading -> Unit
            is Resource.Success -> {
                tvGenres.data?.let { genresList ->
                    val tvGenresList = genresList
                        .map { it.copy(mediaType = "tv") }

                    list = (list + tvGenresList).distinctBy { it.id }
                }
            }
        }

        _genres.update { list }
        _state.update { SearchInitialContentUiState() }
    }

    private suspend fun getGenreThumbnails() {
        if (_genres.value.isNotEmpty()) {
            val list = mutableListOf<Genre>()
            val posterPaths = mutableSetOf<String>()

            list.addAll(_genres.value)
            _genres.value.forEachIndexed { index, genre ->
                when(
                    val result = tmdbRepository.discoverFilms(
                        mediaType = genre.mediaType!!,
                        withGenres = listOf(genre),
                        page = 1,
                        sortBy = SortOptions.POPULARITY
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
                    Resource.Loading -> Unit
                    is Resource.Success -> {
                        result.data?.let { data ->
                            var imageToUse = data.results.random().backdropImage

                            if (imageToUse != null && posterPaths.contains(imageToUse)) {
                                for (item in data.results) {
                                    if (item.backdropImage != null && posterPaths.contains(item.backdropImage)) {
                                        continue
                                    }

                                    imageToUse = item.backdropImage
                                    break
                                }
                            }

                            if(imageToUse == null)
                                return@forEachIndexed

                            posterPaths.add(imageToUse)

                            _genres.update {
                                list[index] = genre.copy(
                                    posterPath = imageToUse
                                )

                                list
                            }
                        }
                    }
                }
            }
        }
    }
}