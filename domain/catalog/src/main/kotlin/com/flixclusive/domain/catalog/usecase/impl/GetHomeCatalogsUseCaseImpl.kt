package com.flixclusive.domain.catalog.usecase.impl

import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.tmdb.model.TMDBHomeCatalog
import com.flixclusive.data.tmdb.repository.TMDBHomeCatalogRepository
import com.flixclusive.domain.catalog.usecase.GetHomeCatalogsUseCase
import com.flixclusive.domain.catalog.util.ProviderCatalogsOperationsHandler
import com.flixclusive.model.provider.Catalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

private const val PREFERRED_MINIMUM_HOME_ITEMS = 15
private const val PREFERRED_MAXIMUM_HOME_ITEMS = 28

internal class GetHomeCatalogsUseCaseImpl
    @Inject
    constructor(
        private val watchProgressRepository: WatchProgressRepository,
        private val tmdbHomeCatalogRepository: TMDBHomeCatalogRepository,
        private val userSessionManager: UserSessionManager,
        providerApiRepository: ProviderApiRepository,
        scope: CoroutineScope,
    ) : GetHomeCatalogsUseCase {
        private val apiChangesHandler = ProviderCatalogsOperationsHandler()

        init {
            scope.launch {
                providerApiRepository.observe().collect {
                    apiChangesHandler.handleOperations(it)
                }
            }
        }

        override suspend operator fun invoke(): Flow<List<Catalog>> {
            return userSessionManager.currentUser.filterNotNull().flatMapLatest { user ->
                combine(
                    apiChangesHandler.catalogs,
                    watchProgressRepository.getRandoms(
                        ownerId = user.id,
                        count = Random.nextInt(1, 4),
                    ),
                ) { providerCatalogs, watchHistoryItems ->
                    val tmdbCatalogs = tmdbHomeCatalogRepository.getAllCatalogs()
                    val allTmdbCatalogs = tmdbCatalogs.all + tmdbCatalogs.tv + tmdbCatalogs.movie
                    val requiredCatalogs = allTmdbCatalogs.filter { it.required }

                    val countOfItemsToFetch = Random.nextInt(
                        from = PREFERRED_MINIMUM_HOME_ITEMS,
                        until = PREFERRED_MAXIMUM_HOME_ITEMS,
                    )

                    val filteredTmdbCatalogs =
                        allTmdbCatalogs
                            .filterNot { it.required }
                            .shuffled()
                            .take(countOfItemsToFetch)

                    val dbFilms = watchHistoryItems.map { it.film }
                    val userRecommendations = buildUserRecommendations(dbFilms)

                    (
                        requiredCatalogs +
                            userRecommendations +
                            filteredTmdbCatalogs +
                            providerCatalogs
                    ).shuffled()
                        .distinctBy { it.name }
                        .sortedByDescending {
                            // Ensure that the trending catalog is always at the top
                            it is TMDBHomeCatalog && it.url.contains("trending/all")
                        }
                }
            }
        }

        private fun buildUserRecommendations(watchHistories: List<DBFilm>): List<TMDBHomeCatalog> {
            return watchHistories.mapNotNull { item ->
                with(item) {
                    if (hasRecommendations && isFromTmdb) {
                        TMDBHomeCatalog(
                            name = "If you liked $title",
                            mediaType = filmType.type,
                            required = false,
                            canPaginate = true,
                            url = "${filmType.type}/${item.id}/recommendations?language=en-US",
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }
