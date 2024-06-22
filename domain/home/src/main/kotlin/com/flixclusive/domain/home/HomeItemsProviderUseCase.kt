package com.flixclusive.domain.home

import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.PagingState
import com.flixclusive.core.util.coroutines.mapIndexedAsync
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.category.CategoryItemsProviderUseCase
import com.flixclusive.domain.provider.SourceLinksProviderUseCase
import com.flixclusive.domain.tmdb.FilmProviderUseCase
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.model.tmdb.category.Category
import com.flixclusive.model.tmdb.category.HomeCategory
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
import kotlin.math.min
import kotlin.random.Random
import com.flixclusive.core.util.R as UtilR

const val PREFERRED_MINIMUM_HOME_ITEMS = 15
internal const val PREFERRED_MAXIMUM_HOME_ITEMS = 28
internal const val HOME_MAX_PAGE = 5

data class PaginationStateInfo(
    val canPaginate: Boolean,
    val pagingState: PagingState,
    val currentPage: Int,
)

@Singleton
class HomeItemsProviderUseCase @Inject constructor(
    private val filmProviderUseCase: FilmProviderUseCase,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val configurationProvider: AppConfigurationManager,
    private val sourceLinksProvider: SourceLinksProviderUseCase,
    private val categoryItemsProviderUseCase: CategoryItemsProviderUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _headerItem = MutableStateFlow<Film?>(null)

    val headerItem = _headerItem.asStateFlow()
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
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
                if (it != null) {
                    _initializationStatus.value = Resource.Failure(it)
                }

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
                    getCategoryItems(
                        category = item,
                        index = i,
                        page = 1,
                    )
                }

                _initializationStatus.value = getHeaderItem()
            }.collect()
        }
    }

    suspend fun getCategoryItems(
        category: Category,
        index: Int,
        page: Int,
    ) {
        when (
            val result = categoryItemsProviderUseCase(
                category = category,
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
     * Since [getHeaderItem] is the only method that calls to an api service,
     * [isInitialized] depends on it whether if [getHeaderItem] was successful or not.
     *
     * @return a state whether the call was successfully or not.
     * */
    private suspend fun getHeaderItem(): Resource<Unit> {
        var headerItem: Film? = null
        val traversedFilms = mutableMapOf<String, String>()
        var lastResponse: Resource<Unit> =
            Resource.Failure(UtilR.string.failed_to_initialize_home_items)

        for (i in 0..5) {
            headerItem = null

            while (headerItem == null || headerItem.isNotPopular) {
                if (traversedFilms.contains(headerItem?.identifier)) {
                    headerItem = null
                    continue
                }

                headerItem = (rowItems.value
                    .randomOrNull()
                    ?.randomOrNull() as FilmSearchItem?)
                    ?.also {
                        traversedFilms[it.identifier] = it.title
                    }

            }

            val response = filmProviderUseCase(partiallyDetailedFilm = headerItem)
            if (response is Resource.Failure) {
                lastResponse = response
                continue
            }

            response.data?.let {
                val genres = it.genres.plus(headerItem?.genres ?: emptyList())

                headerItem = when (it) {
                    is Movie -> it.copy(genres = genres)
                    is TvShow -> it.copy(genres = genres)
                    else -> null
                }
            }

            if (headerItem == null)
                continue
        }

        _headerItem.value = headerItem

        return when (headerItem) {
            null -> lastResponse
            else -> Resource.Success(Unit)
        }
    }

    private suspend fun getHomeRecommendations() = flow {
        val usedCategories = mutableListOf<String>()

        val tmdbCategories = configurationProvider.homeCategoriesData!!

        val allCategories = tmdbCategories.tv + tmdbCategories.movie + getProviderCatalogs()
        val allTmdbCategories = tmdbCategories.all + tmdbCategories.tv + tmdbCategories.movie

        val maxPossibleSize = allCategories.size + tmdbCategories.all.size

        val minItemsToFetch = min(maxPossibleSize, PREFERRED_MINIMUM_HOME_ITEMS)
        val maxItemsToFetch = min(maxPossibleSize, PREFERRED_MAXIMUM_HOME_ITEMS)

        var countOfItemsToFetch = safeCall {
            Random.nextInt(minItemsToFetch, maxItemsToFetch)
        } ?: maxPossibleSize
        var i = 0
        while (i < countOfItemsToFetch) {
            if (i >= maxItemsToFetch) {
                break
            }

            val shouldEmitRequiredCategories = Random.nextBoolean()
            var item = allCategories.random()

            if (shouldEmitRequiredCategories) {
                val requiredRecommendation = allTmdbCategories.find {
                    !usedCategories.contains(it.name) && it.required
                }

                when {
                    requiredRecommendation != null -> {
                        countOfItemsToFetch++
                        item = requiredRecommendation
                    }
                    item is ProviderCatalog -> countOfItemsToFetch++
                }
            }

            if (usedCategories.contains(item.name))
                continue

            emit(item)
            i++
            usedCategories.add(item.name)
            humanizer()
        }
    }

    private fun getProviderCatalogs(): List<ProviderCatalog> {
        val provideWithCatalogs: List<ProviderCatalog>
            = sourceLinksProvider.providerApis
                .flatMap { it.catalogs }

        return provideWithCatalogs
    }

    private fun getUserRecommendations(userId: Int = 1) = flow {
        val randomWatchedFilms =
            watchHistoryRepository.getRandomWatchHistoryItems(
                ownerId = userId,
                count = Random.nextInt(1, 4)
            )

        randomWatchedFilms.forEach { item ->
            with(item.film) {
                if (recommendations.size >= 10 && isFromTmdb) {
                    emit(
                        HomeCategory(
                            name = "If you liked $title",
                            mediaType = filmType.type,
                            required = false,
                            canPaginate = true,
                            url = "${filmType.type}/${item.id}/recommendations?language=en-US"
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

    private val Film?.isNotPopular: Boolean
        get() = safeCall {
            (this as? FilmSearchItem)?.run {
                return@run isFromTmdb && voteCount < 200
            }!!
        } ?: false
}