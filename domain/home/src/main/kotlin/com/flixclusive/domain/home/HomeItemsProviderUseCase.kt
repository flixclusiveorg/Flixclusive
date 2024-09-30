package com.flixclusive.domain.home

import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.ui.common.util.PagingState
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.launchOnIO
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withDefaultContext
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.provider.ProviderManager
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.domain.catalog.CatalogItemsProviderUseCase
import com.flixclusive.domain.tmdb.FilmProviderUseCase
import com.flixclusive.model.configuration.catalog.HomeCatalog
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
    private val catalogItemsProviderUseCase: CatalogItemsProviderUseCase,
    private val providerManager: ProviderManager
) {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        observeConfigurationAndProviders()
    }

    operator fun invoke() {
        launchOnIO {
            _state.update { it.copy(status = Resource.Loading) }

            try {
                val catalogs = withDefaultContext { getHomeRecommendations() }
                _state.update {
                    it.copy(
                        catalogs = catalogs,
                        rowItems = List(catalogs.size) { emptyList() },
                        rowItemsPagingState = withDefaultContext {
                            catalogs.map { catalog ->
                                PaginationStateInfo(
                                    canPaginate = catalog.canPaginate,
                                    pagingState = if (!catalog.canPaginate) PagingState.PAGINATING_EXHAUST else PagingState.IDLE,
                                    currentPage = 1
                                )
                            }
                        }
                    )
                }

                val headerItem = getHeaderItem(catalogs)
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

    suspend fun getCatalogItems(
        catalog: Catalog,
        index: Int,
        page: Int
    ) {
        when (val result = catalogItemsProviderUseCase(catalog, page)) {
            is Resource.Success -> handleCatalogItemsSuccess(result.data!!, catalog, index, page)
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

        launchOnIO {
            catalogs.collectLatest {
                _state.update {  homeState ->
                    homeState.copy(providerCatalogs = it)
                }

                invoke()
            }
        }
    }

    private suspend fun getHomeRecommendations(): List<Catalog> {
        val tmdbCatalogs = configurationProvider.homeCatalogsData!!
        val allTmdbCatalogs = tmdbCatalogs.all + tmdbCatalogs.tv + tmdbCatalogs.movie
        val requiredCatalogs = allTmdbCatalogs.filter { it.required }

        val countOfItemsToFetch = Random.nextInt(PREFERRED_MINIMUM_HOME_ITEMS, PREFERRED_MAXIMUM_HOME_ITEMS)
        val filteredTmdbCatalogs = allTmdbCatalogs
            .filterNot { it.required }
            .shuffled()
            .take(countOfItemsToFetch)

        return (requiredCatalogs +
                getUserRecommendations() +
                filteredTmdbCatalogs +
                state.value.providerCatalogs)
            .shuffled()
            .distinctBy { it.name }
            .sortedByDescending {
                it is HomeCatalog && it.url.contains("trending/all")
            }
    }

    private suspend fun getUserRecommendations(userId: Int = 1): List<HomeCatalog> {
        val randomWatchedFilms = watchHistoryRepository.getRandomWatchHistoryItems(
            ownerId = userId,
            count = Random.nextInt(1, 4)
        )

        return randomWatchedFilms.mapNotNull { item ->
            with(item.film) {
                if (recommendations.size >= 10 && isFromTmdb) {
                    HomeCatalog(
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

    private suspend fun getHeaderItem(catalogs: List<Catalog>): Film? {
        val traversedCatalogs = mutableSetOf<Int>()
        val traversedFilms = mutableSetOf<String>()

        for (attempt in 0..5) {
            var headerItem: Film? = null
            var catalogAttempts = 0

            while (headerItem == null && catalogAttempts < HOME_MAX_PAGE) {
                val randomIndex = Random.nextInt(catalogs.size)
                val catalog = catalogs[randomIndex]

                if (catalog !is ProviderCatalog && !traversedCatalogs.contains(randomIndex)) {
                    getCatalogItems(catalog, randomIndex, 1)
                    traversedCatalogs.add(randomIndex)
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
                catalogAttempts++
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

    private fun handleCatalogItemsSuccess(
        data: SearchResponseData<FilmSearchItem>,
        catalog: Catalog,
        index: Int,
        page: Int
    ) {
        val maxPage = minOf(HOME_MAX_PAGE, data.totalPages)
        val canPaginate = data.results.size == 20 && page < maxPage && catalog.canPaginate

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