package com.flixclusive.domain.home

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.data.watch_history.WatchHistoryRepository
import com.flixclusive.model.configuration.HomeCategoryItem
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.Genre
import com.flixclusive.model.tmdb.TMDBPageResponse
import com.flixclusive.model.tmdb.TMDBSearchItem
import com.flixclusive.model.tmdb.util.formatGenreIds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random

private const val MINIMUM_HOME_ITEMS = 15
private const val MAXIMUM_HOME_ITEMS = 28

class HomeItemsProviderUseCase @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val configurationProvider: AppConfigurationManager,
) {

    suspend fun getFocusedItem(film: Film): Film? {
        val response: Resource<Film> = when (film.filmType) {
            FilmType.MOVIE -> tmdbRepository.getMovie(film.id)
            FilmType.TV_SHOW -> tmdbRepository.getTvShow(film.id)
        }

        if (response is Resource.Failure)
            return null

        return response.data
    }

    suspend fun getHeaderItem(): Film? {
        var headerItemToUse: TMDBSearchItem? = null

        retry@ for(i in 0..5) {
            val page = Random.nextInt(1, 3000)
            val itemConfig =
                if (page % 2 == 0) configurationProvider.homeCategoriesConfig?.movie?.random() else configurationProvider.homeCategoriesConfig?.tv?.random() ?: continue

            val response = tmdbRepository.paginateConfigItems(
                url = itemConfig!!.query,
                page = Integer.max(1, page % 5)
            )

            if (response is Resource.Failure)
                continue

            if(response.data?.results?.isEmpty() == true)
                continue

            response.data?.results?.size?.let {
                headerItemToUse = response.data!!.results.random()
            }

            if (headerItemToUse != null) {
                val item = headerItemToUse!! // For smart casting

                val imageResponse = tmdbRepository.getImages(
                    mediaType = if (page % 2 == 0) FilmType.MOVIE.type else FilmType.TV_SHOW.type,
                    id = item.id
                )

                if (imageResponse is Resource.Failure)
                    continue

                if (imageResponse.data?.logos != null) {
                    val logos = imageResponse.data!!.logos

                    if (logos.isEmpty()) {
                        continue
                    }

                    val logoToUse = logos[0].filePath.replace("svg", "png")

                    when (item) {
                        is TMDBSearchItem.MovieTMDBSearchItem -> {
                            val newGenres = formatGenreIds(
                                genreIds = item.genreIds,
                                genresList = configurationProvider
                                    .searchCategoriesConfig!!.genres.map {
                                        Genre(
                                            id = it.id,
                                            name = it.name,
                                            mediaType = it.mediaType
                                        )
                                    }
                            ) + item.genres

                            headerItemToUse = item.copy(
                                logoImage = logoToUse,
                                genres = newGenres
                            )
                            break
                        }
                        is TMDBSearchItem.TvShowTMDBSearchItem -> {
                            val newGenres = formatGenreIds(
                                genreIds = item.genreIds,
                                genresList = configurationProvider
                                    .searchCategoriesConfig!!.genres.map {
                                        Genre(
                                            id = it.id,
                                            name = it.name,
                                            mediaType = FilmType.TV_SHOW.type
                                        )
                                    }
                            ) + item.genres

                            headerItemToUse = item.copy(
                                logoImage = logoToUse,
                                genres = newGenres
                            )
                            break
                        }

                        else -> throw IllegalStateException("SearchItem is not parsable to a film!")
                    }
                }
            }
        }

        return headerItemToUse
    }

    fun getHomeRecommendations(): Flow<HomeCategoryItem?> = flow {
        val usedCategories = mutableListOf<String>()

        val config = configurationProvider.homeCategoriesConfig!!

        val combinedMovieAndTvShowConfig = config.tv + config.movie
        val combinedConfig = config.all + combinedMovieAndTvShowConfig

        var countOfItemsToFetch = Random.nextInt(MINIMUM_HOME_ITEMS, MAXIMUM_HOME_ITEMS)
        var i = 0
        while (i < countOfItemsToFetch) {
            val shouldEmitRequiredCategories = Random.nextInt(0, 1000) % Random.nextInt(2, 3) == 0

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
            usedCategories.add(item.name)
            i++
            humanizer()
        }
    }

    suspend fun getHomeItems(
        query: String,
        page: Int,
        onFailure: () -> Unit,
        onSuccess: (data: TMDBPageResponse<TMDBSearchItem>) -> Unit,
    ) {
        when (
            val result = tmdbRepository.paginateConfigItems(
                url = query,
                page = page
            )
        ) {
            is Resource.Failure -> onFailure()
            Resource.Loading -> Unit
            is Resource.Success -> onSuccess(result.data!!)
        }
    }

    fun getUserRecommendations(userId: Int, count: Int): Flow<HomeCategoryItem?> = flow {
        check(count > 0) { "SearchItem count must be greater than 0" }

        val randomWatchedFilms =
            watchHistoryRepository.getRandomWatchHistoryItems(ownerId = userId, count = count)
        if (randomWatchedFilms.isNotEmpty()) {
            randomWatchedFilms.forEach { item ->
                if (item.film.recommendedTitles.size >= 10) {
                    emit(
                        HomeCategoryItem(
                            name = "If you liked ${item.film.title}",
                            mediaType = item.film.filmType.type,
                            required = false,
                            canPaginate = true,
                            query = "${item.film.filmType.type}/${item.id}/recommendations?language=en-US"
                        )
                    )
                    humanizer()
                }
            }
        }
    }

    private suspend fun humanizer() {
        delay(Random.nextLong(0, 30))
    }
}