package com.flixclusive.domain.home

import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.PagingState
import com.flixclusive.core.util.coroutines.mapIndexedAsync
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.tmdb.FilmProviderUseCase
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.Genre
import com.flixclusive.model.tmdb.category.HomeCategory
import com.flixclusive.model.tmdb.util.formatGenreIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import com.flixclusive.core.util.R as UtilR

const val MINIMUM_HOME_ITEMS = 15
private const val MAXIMUM_HOME_ITEMS = 28
private const val HOME_MAX_PAGE = 5

data class PaginationStateInfo(
    val canPaginate: Boolean,
    val pagingState: PagingState,
    val currentPage: Int,
)

@Singleton
class HomeItemsProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    private val filmProviderUseCase: FilmProviderUseCase,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val configurationProvider: AppConfigurationManager,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _headerItem = MutableStateFlow<Film?>(null)

    val headerItem = _headerItem.asStateFlow()
    private val _categories = MutableStateFlow<List<HomeCategory>>(emptyList())
    private val _rowItems = MutableStateFlow<List<List<Film>>>(emptyList())
    private val _rowItemsPagingState = MutableStateFlow<List<PaginationStateInfo>>(emptyList())

    private val _initializationStatus = MutableStateFlow<Resource<Unit>>(Resource.Loading)
    val initializationStatus = _initializationStatus.asStateFlow()
    private var initializeJob: Job? = null
    val rowItemsPaginationJobs = mutableListOf<Job?>()

    val categories = _categories.asStateFlow()
    val rowItems = _rowItems.asStateFlow()
    val rowItemsPagingState = _rowItemsPagingState.asStateFlow()

    operator fun invoke() {
        if (initializeJob?.isActive == true)
            return

        initializeJob = scope.launch {
            _initializationStatus.value = Resource.Loading

            // Reset all state
            _headerItem.value = null
            _categories.value = emptyList()
            _rowItems.value = emptyList()
            _rowItemsPagingState.value = emptyList()
            rowItemsPaginationJobs.clear()

            // Async call these for humanization purposes
            merge(
                getHomeRecommendations(),
                getUserRecommendations()
            ).onEach { item ->
                _categories.value += listOf(item)
            }.onCompletion {
                rowItemsPaginationJobs.addAll(List(_categories.value.size) { null })
                _rowItems.value = List(_categories.value.size) { emptyList() }
                _rowItemsPagingState.value = _categories.value.map { item ->
                    PaginationStateInfo(
                        canPaginate = item.canPaginate,
                        pagingState = if (!item.canPaginate) PagingState.PAGINATING_EXHAUST else PagingState.IDLE,
                        currentPage = 1
                    )
                }

                _categories.value.mapIndexedAsync { i, item ->
                    getHomeItems(
                        index = i,
                        query = item.url,
                        page = 1,
                    )
                }

                /**
                 * Since [getHeaderItem] is the only method that calls to an api service,
                 * [isInitialized] depends on it whether if [getHeaderItem] was successful or not.
                 * */
                _initializationStatus.value = getHeaderItem()
            }.collect()
        }
    }

    suspend fun getHomeItems(
        index: Int,
        query: String,
        page: Int,
    ) {
        when (
            val result = tmdbRepository.paginateConfigItems(
                url = query,
                page = page
            )
        ) {
            is Resource.Failure -> {
                updatePagingState(
                    index = index,
                    updateBlock = {
                        it.copy(pagingState = PagingState.ERROR)
                    }
                )
            }

            Resource.Loading -> Unit
            is Resource.Success -> {
                result.data!!.run {
                    val maxPage = minOf(HOME_MAX_PAGE, totalPages)
                    val canPaginate = results.size == 20 && page < maxPage

                    val newFilmsList = _rowItems.value[index].toMutableList()

                    if (page == 1) {
                        newFilmsList.clear()
                    }

                    newFilmsList.addAll(
                        results.filter { item ->
                            !newFilmsList.contains(item)
                        }
                    )

                    val newList = _rowItems.value.toMutableList()
                    newList[index] = newFilmsList.toList()
                    _rowItems.value = newList.toList()

                    updatePagingState(
                        index = index,
                        updateBlock = {
                            it.copy(
                                canPaginate = canPaginate,
                                pagingState = if (canPaginate) PagingState.IDLE else PagingState.PAGINATING_EXHAUST,
                                currentPage = if (canPaginate) page + 1 else page
                            )
                        }
                    )
                }
            }
        }
    }

    /**
     *
     * Obtains the information of the focused film.
     * This is used for the immersive carousel on android TV
     * */
    suspend fun getFocusedFilm(film: Film) {
        _headerItem.update {
            filmProviderUseCase(partiallyDetailedFilm = film).data
        }
    }

    /**
     * Obtains the home header item for mobile.
     *
     * @return a state whether the call was successfull or not.
     * */
    private suspend fun getHeaderItem(): Resource<Unit> {
        var headerItemToUse: FilmSearchItem? = null
        var lastResponse: Resource<Unit> =
            Resource.Failure(UtilR.string.failed_to_initialize_home_items)

        retry@ for (i in 0..5) {
            headerItemToUse = rowItems.value.randomOrNull()?.randomOrNull() as FilmSearchItem?

            if (headerItemToUse == null)
                continue

            val logo = tmdbRepository.getLogo(
                mediaType = headerItemToUse.filmType.type,
                id = headerItemToUse.tmdbId ?: continue
            )

            if (logo is Resource.Failure) {
                lastResponse = logo
                continue
            }

            if (logo.data != null) {
                val logoToUse = logo.data

                val newGenres = formatGenreIds(
                    genreIds = headerItemToUse.genreIds,
                    genresList = configurationProvider
                        .searchCategoriesData!!.genres.map {
                            val mediaType = when (headerItemToUse!!.filmType) {
                                FilmType.MOVIE -> it.mediaType
                                FilmType.TV_SHOW -> FilmType.TV_SHOW.type
                            }

                            Genre(
                                id = it.id,
                                name = it.name,
                                mediaType = mediaType
                            )
                        }
                ) + headerItemToUse.genres

                headerItemToUse = headerItemToUse.copy(
                    logoImage = logoToUse,
                    genres = newGenres
                )
            }
        }

        _headerItem.value = headerItemToUse

        return when (headerItemToUse) {
            null -> lastResponse
            else -> Resource.Success(Unit)
        }
    }

    private suspend fun getHomeRecommendations() = flow {
        val usedCategories = mutableListOf<String>()

        val config = configurationProvider.homeCategoriesData!!

        val combinedMovieAndTvShowConfig = config.tv + config.movie
        val combinedConfig = config.all + combinedMovieAndTvShowConfig

        var countOfItemsToFetch = Random.nextInt(MINIMUM_HOME_ITEMS, MAXIMUM_HOME_ITEMS)
        var i = 0
        while (i < countOfItemsToFetch) {
            val shouldEmitRequiredCategories = Random.nextBoolean()

            val item = if (shouldEmitRequiredCategories) {
                val requiredRecommendation = combinedConfig.find {
                    !usedCategories.contains(it.name) && it.required
                }

                if (requiredRecommendation != null) {
                    countOfItemsToFetch++
                    requiredRecommendation
                } else combinedMovieAndTvShowConfig.random()
            } else combinedMovieAndTvShowConfig.random()

            if (usedCategories.contains(item.name))
                continue

            emit(item)
            i++
            usedCategories.add(item.name)
            humanizer()
        }
    }

    private fun getUserRecommendations(userId: Int = 1) = flow {
        val randomWatchedFilms =
            watchHistoryRepository.getRandomWatchHistoryItems(
                ownerId = userId,
                count = Random.nextInt(1, 4)
            )

        if (randomWatchedFilms.isNotEmpty()) {
            randomWatchedFilms.forEach { item ->
                if (item.film.recommendations.size >= 10) {
                    emit(
                        HomeCategory(
                            name = "If you liked ${item.film.title}",
                            mediaType = item.film.filmType.type,
                            required = false,
                            canPaginate = true,
                            url = "${item.film.filmType.type}/${item.id}/recommendations?language=en-US"
                        )
                    )
                    humanizer()
                }
            }
        }
    }

    /**
     *
     * Random pauses for humanizing effect
     * */
    private suspend fun humanizer() {
        delay(Random.nextLong(0, 100))
    }

    private fun updatePagingState(
        index: Int,
        updateBlock: (PaginationStateInfo) -> PaginationStateInfo
    ) {
        val newList = _rowItemsPagingState.value.toMutableList()
        newList[index] = updateBlock(newList[index])

        _rowItemsPagingState.value = newList.toList()
    }
}