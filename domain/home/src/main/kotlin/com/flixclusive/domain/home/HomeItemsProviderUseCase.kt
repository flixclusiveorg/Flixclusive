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
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.model.tmdb.category.Category
import com.flixclusive.model.tmdb.category.HomeCategory
import kotlinx.coroutines.CoroutineScope
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
    private val providerManager: ProviderManager,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        observeConfigurationAndProviders()
    }

    operator fun invoke() {
        scope.launch {
            _state.update { it.copy(status = Resource.Loading) }
            try {
                val categories = getHomeRecommendations()
                _state.update {
                    it.copy(
                        categories = categories,
                        rowItems = List(categories.size) { emptyList() },
                        rowItemsPagingState = categories.map { category ->
                            PaginationStateInfo(
                                canPaginate = category.canPaginate,
                                pagingState = if (!category.canPaginate) PagingState.PAGINATING_EXHAUST else PagingState.IDLE,
                                currentPage = 1
                            )
                        }
                    )
                }

                val headerItem = getHeaderItem(categories)
                _state.update {
                    it.copy(
                        headerItem = headerItem!!,
                        status = Resource.Success(Unit)
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(status = Resource.Failure(e)) }
            }
        }
    }

    suspend fun getCategoryItems(
        category: Category,
        index: Int,
        page: Int
    ) {
        when (val result = categoryItemsProviderUseCase(category, page)) {
            is Resource.Success -> handleCategoryItemsSuccess(result.data!!, category, index, page)
            is Resource.Failure -> updatePagingState(index) { it.copy(pagingState = PagingState.ERROR) }
            Resource.Loading -> Unit
        }
    }

    suspend fun getFocusedFilm(film: Film) {
        _state.update {
            val focusedFilm = filmProviderUseCase(partiallyDetailedFilm = film).data
            it.copy(headerItem = focusedFilm)
        }
    }

    private fun observeConfigurationAndProviders() {
        val catalogs = providerManager.workingApis.map {
            it.flatMap { api -> api.catalogs }
        }.distinctUntilChanged()

        scope.launch {
            configurationProvider.configurationStatus
                .combine(catalogs) { configStatus, catalogs ->
                    configStatus to catalogs
                }
                .collectLatest { (configStatus, catalogs) ->
                    if (configStatus is Resource.Success) {
                        _state.update { it.copy(providerCatalogs = catalogs) }
                        invoke()
                    }
                }
        }
    }

    private suspend fun getHomeRecommendations(): List<Category> {
        val tmdbCategories = configurationProvider.homeCategoriesData!!
        val allTmdbCategories = tmdbCategories.all + tmdbCategories.tv + tmdbCategories.movie
        val requiredCategories = allTmdbCategories.filter { it.required }

        val countOfItemsToFetch = Random.nextInt(PREFERRED_MINIMUM_HOME_ITEMS, PREFERRED_MAXIMUM_HOME_ITEMS)
        val filteredTmdbCategories = allTmdbCategories
            .filterNot { it.required }
            .shuffled()
            .take(countOfItemsToFetch)

        return (requiredCategories +
                getUserRecommendations() +
                filteredTmdbCategories +
                state.value.providerCatalogs)
            .shuffled()
            .distinctBy { it.name }
            .sortedByDescending {
                it is HomeCategory && it.url.contains("trending/all")
            }
    }

    private suspend fun getUserRecommendations(userId: Int = 1): List<HomeCategory> {
        val randomWatchedFilms = watchHistoryRepository.getRandomWatchHistoryItems(
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
    private suspend fun getHeaderItem(categories: List<Category>): Film? {
        val traversedCategories = mutableSetOf<Int>()
        val traversedFilms = mutableSetOf<String>()

        for (attempt in 0..5) {
            var headerItem: Film? = null
            var categoryAttempts = 0

            while (headerItem == null && categoryAttempts < HOME_MAX_PAGE) {
                val randomIndex = Random.nextInt(categories.size)
                val category = categories[randomIndex]

                if (category !is ProviderCatalog && !traversedCategories.contains(randomIndex)) {
                    getCategoryItems(category, randomIndex, 1)
                    traversedCategories.add(randomIndex)
                }

                headerItem = state.value.rowItems.getOrNull(randomIndex)?.randomOrNull()

                if (headerItem != null && !traversedFilms.contains(headerItem.identifier)) {
                    traversedFilms.add(headerItem.identifier)

                    if (!headerItem.isNotPopular) {
                        val response = filmProviderUseCase(partiallyDetailedFilm = headerItem)
                        if (response is Resource.Success) {
                            return enhanceHeaderItem(headerItem, response.data)
                        }
                    }
                }

                headerItem = null
                categoryAttempts++
            }
        }

        return null
    }

    private fun enhanceHeaderItem(originalItem: Film, enhancedItem: Film?): Film? {
        if (enhancedItem == null) return null

        val genres = enhancedItem.genres + originalItem.genres
        return when (enhancedItem) {
            is Movie -> enhancedItem.copy(genres = genres)
            is TvShow -> enhancedItem.copy(genres = genres)
            else -> null
        }
    }

    private fun handleCategoryItemsSuccess(
        data: SearchResponseData<FilmSearchItem>,
        category: Category,
        index: Int,
        page: Int
    ) {
        val maxPage = minOf(HOME_MAX_PAGE, data.totalPages)
        val canPaginate = data.results.size == 20 && page < maxPage && category.canPaginate

        _state.update { currentState ->
            val updatedRowItems = currentState.rowItems.toMutableList()
            updatedRowItems[index] = if (page == 1) {
                data.results
            } else {
                (currentState.rowItems[index] + data.results).distinctBy { it.identifier }
            }

            val updatedPagingState = currentState.rowItemsPagingState.toMutableList()
            updatedPagingState[index] = PaginationStateInfo(
                canPaginate = canPaginate,
                pagingState = if (canPaginate) PagingState.IDLE else PagingState.PAGINATING_EXHAUST,
                currentPage = if (canPaginate) page + 1 else page
            )

            currentState.copy(
                rowItems = updatedRowItems,
                rowItemsPagingState = updatedPagingState
            )
        }
    }

    private fun updatePagingState(
        index: Int,
        updateBlock: (PaginationStateInfo) -> PaginationStateInfo
    ) {
        _state.update { currentState ->
            val updatedPagingState = currentState.rowItemsPagingState.toMutableList()
            updatedPagingState[index] = updateBlock(updatedPagingState[index])
            currentState.copy(rowItemsPagingState = updatedPagingState)
        }
    }

    private val Film.isNotPopular: Boolean
        get() = safeCall {
            (this as? FilmSearchItem)?.run {
                isFromTmdb && voteCount < 250
            } ?: false
        } ?: false
}