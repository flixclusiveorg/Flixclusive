package com.flixclusive.domain.home

import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.common.ui.PagingState
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.category.CategoryItemsProviderUseCase
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
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
    private val categoryItemsProviderUseCase: CategoryItemsProviderUseCase,
    providerManager: ProviderManager,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _headerItem = MutableStateFlow<Film?>(null)
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    private val _rowItems = MutableStateFlow<List<List<Film>>>(emptyList())
    private val _rowItemsPagingState = MutableStateFlow<List<PaginationStateInfo>>(emptyList())

    private val _initializationStatus = MutableStateFlow<Resource<Unit>>(Resource.Loading)
    val initializationStatus = _initializationStatus.asStateFlow()

    private var initializeJob: Job? = null
    val rowItemsPaginationJobs = mutableListOf<Job?>()

    val headerItem = _headerItem.asStateFlow()
    val categories = _categories.asStateFlow()
    val rowItems = _rowItems.asStateFlow()
    val rowItemsPagingState = _rowItemsPagingState.asStateFlow()

    private var providerCatalogs: List<ProviderCatalog> = emptyList()

    init {
        val providerCatalogs = providerManager.workingApis
            .map { list ->
                list.flatMap { it.catalogs }
            }.distinctUntilChanged()

        scope.launch {
            configurationProvider.configurationStatus
                .combine(providerCatalogs) { configurationStatus, catalogs ->
                    configurationStatus to catalogs
                }.collectLatest { (configurationStatus, catalogs) ->
                    if (configurationStatus is Resource.Success) {
                        this@HomeItemsProviderUseCase.providerCatalogs = catalogs

                        // Force initialize the home items
                        initializeJob?.cancel()
                        invoke()
                    }
                }
        }
    }

    operator fun invoke() {
        if (initializeJob?.isActive == true)
            return

        initializeJob = scope.launch {
            _initializationStatus.value = Resource.Loading

            val allCategories = getHomeRecommendations()
            
            _categories.value = allCategories
            rowItemsPaginationJobs.clear()
            rowItemsPaginationJobs.addAll(List(allCategories.size) { null })
            _rowItems.value = List(allCategories.size) { emptyList() }
            _rowItemsPagingState.value = _categories.value.map { item ->
                PaginationStateInfo(
                    canPaginate = item.canPaginate,
                    pagingState = if (!item.canPaginate) PagingState.PAGINATING_EXHAUST else PagingState.IDLE,
                    currentPage = 1
                )
            }

            _initializationStatus.value = getHeaderItem()
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
                    val canPaginate = results.size == 20 && page < maxPage && category.canPaginate

                    val filmsMap = _rowItems.value[index]
                        .associateByTo(mutableMapOf()) { it.identifier }
                    val existingFilms = filmsMap.keys.toHashSet()

                    if (page == 1) {
                        filmsMap.clear()
                        existingFilms.clear()
                    }

                    results.forEach { item ->
                        if (existingFilms.add(item.identifier)) {
                            filmsMap[item.identifier] = item
                        }
                    }

                    val newList = _rowItems.value.toMutableList()
                    newList[index] = filmsMap.values.toList()
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
        val traversedCategories = mutableMapOf<Int, Category>()
        val traversedFilms = mutableMapOf<String, String>()
        var lastResponse: Resource<Unit> =
            Resource.Failure(UtilR.string.failed_to_initialize_home_items)

        for (i in 0..5) {
            headerItem = null
            var attempts = 0

            while (
                (headerItem == null
                || headerItem.isNotPopular)
                && attempts < HOME_MAX_PAGE
            ) {
                val randomIndex = Random.nextInt(_categories.value.size)
                val category = _categories.value[randomIndex]

                if (category is ProviderCatalog) {
                    traversedCategories[randomIndex] = category
                    continue
                }

                if (!traversedCategories.containsKey(randomIndex)) {
                    getCategoryItems(
                        category = category,
                        index = randomIndex,
                        page = 1
                    )
                }

                traversedCategories[randomIndex] = category

                headerItem = rowItems.value[randomIndex].randomOrNull()

                if (traversedFilms.containsKey(headerItem?.identifier)) {
                    headerItem = null
                    continue
                }

                headerItem?.let {
                    traversedFilms[it.identifier] = it.title
                }

                attempts++
            }

            if (headerItem == null || headerItem.isNotPopular) {
                continue
            }

            val response = filmProviderUseCase(partiallyDetailedFilm = headerItem)
            if (response is Resource.Failure) {
                lastResponse = response
                continue
            }

            response.data?.let {
                val genres = it.genres.plus(
                    elements = headerItem?.genres ?: emptyList()
                )

                headerItem = when (it) {
                    is Movie -> it.copy(genres = genres)
                    is TvShow -> it.copy(genres = genres)
                    else -> null
                }
            }
        }

        _headerItem.value = headerItem

        return when (headerItem) {
            null -> lastResponse
            else -> Resource.Success(Unit)
        }
    }

    private suspend fun getHomeRecommendations(): List<Category> {
        val tmdbCategories = configurationProvider.homeCategoriesData!!

        val allTmdbCategories = tmdbCategories.all + tmdbCategories.tv + tmdbCategories.movie
        val requiredCategories = allTmdbCategories.filter { it.required }

        val countOfItemsToFetch = Random.nextInt(PREFERRED_MINIMUM_HOME_ITEMS, PREFERRED_MAXIMUM_HOME_ITEMS)
        val filteredTmdbCategories = allTmdbCategories
            .filterNot { it.required }
            .shuffled().take(countOfItemsToFetch)
        
        return (requiredCategories +
            getUserRecommendations() +
            filteredTmdbCategories +
            providerCatalogs).shuffled()
            .distinctBy { it.name }
            .sortedByDescending {
                val isTrendingCategory = it is HomeCategory && it.url.contains("trending/all")
                isTrendingCategory
            }
    }

    private suspend fun getUserRecommendations(userId: Int = 1): List<HomeCategory> {
        val randomWatchedFilms =
            watchHistoryRepository.getRandomWatchHistoryItems(
                ownerId = userId,
                count = Random.nextInt(1, 4)
            )

        return randomWatchedFilms.mapNotNull { item ->
            with(item.film) {
                if (recommendations.size >= 10 && isFromTmdb) {
                    HomeCategory(
                        name = "If you liked $title",
                        mediaType = filmType.type,
                        required = false,
                        canPaginate = true,
                        url = "${filmType.type}/${item.id}/recommendations?language=en-US"
                    )
                } else null
            }
        }
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
                return@run isFromTmdb && voteCount < 250
            }!!
        } ?: false
}