package com.flixclusive.data.usecase

import com.flixclusive.R
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.GENRES_LIST
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.model.tmdb.WatchProvider
import com.flixclusive.domain.repository.SortOptions
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.domain.repository.WatchHistoryRepository
import com.flixclusive.domain.usecase.HomeItemConfig
import com.flixclusive.domain.usecase.HomeItemsProviderUseCase
import com.flixclusive.domain.usecase.POPULAR_MOVIE_FLAG
import com.flixclusive.domain.usecase.POPULAR_TV_FLAG
import com.flixclusive.domain.usecase.TOP_MOVIE_FLAG
import com.flixclusive.domain.usecase.TOP_TV_FLAG
import com.flixclusive.domain.usecase.TRENDING_FLAG
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.common.toTitleCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random.Default.nextInt

class HomeItemsProviderUseCaseImpl @Inject constructor(
    private val tmdbRepository: TMDBRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) : HomeItemsProviderUseCase {
    override suspend fun getHeaderItem(): Film? {
        var headerItemToUse: TMDBSearchItem? = null

        val page = nextInt(1, 3)

        val listOfMediaTypes = listOf("movie", "tv")
        val mediaTypeToUse = listOfMediaTypes[nextInt(0, 2)]
        val response = tmdbRepository.discoverFilms(
            mediaType = mediaTypeToUse,
            page = page,
            sortBy = SortOptions.POPULARITY
        )
        if(response is Resource.Failure)
            return null

        response.data?.results?.size?.let {
            headerItemToUse = response.data.results.random()
        }

        headerItemToUse?.let { item ->
            val imageResponse = tmdbRepository.getImages(
                mediaType = mediaTypeToUse,
                id = item.id
            )
            if(imageResponse is Resource.Failure)
                return null

            imageResponse.data?.logos?.let { logos ->
                if(logos.isEmpty()) {
                    return getHeaderItem()
                }

                headerItemToUse = when(item) {
                    is TMDBSearchItem.MovieTMDBSearchItem -> item.copy(logoImage = logos[0].filePath.replace("svg", "png"))
                    is TMDBSearchItem.TvShowTMDBSearchItem -> item.copy(logoImage = logos[0].filePath.replace("svg", "png"))
                    is TMDBSearchItem.PersonTMDBSearchItem -> throw IllegalStateException("Item should not be a person!")
                }
            }
        }

        return headerItemToUse
    }

    override fun getMainRowItems(): Flow<HomeItemConfig?> = flow {
        val errorResult = null

        val trendingAll = tmdbRepository.getTrending(page = 1)
        if (trendingAll is Resource.Failure)
            return@flow emit(errorResult)

        emit(
            HomeItemConfig(
                flag = TRENDING_FLAG,
                label = UiText.StringResource(R.string.trending),
                data = trendingAll.data!!.results
            )
        )

        val topMovies = tmdbRepository.getTrending(
            mediaType = FilmType.MOVIE.type,
            page = 1
        )
        if (topMovies is Resource.Failure)
            return@flow emit(errorResult)

        emit(
            HomeItemConfig(
                flag = TOP_MOVIE_FLAG,
                label = UiText.StringResource(R.string.top_movies_recently),
                data = topMovies.data!!.results
            )
        )

        val topTvShows = tmdbRepository.getTrending(
            mediaType = FilmType.TV_SHOW.type,
            page = 1
        )
        if (topTvShows is Resource.Failure)
            return@flow emit(errorResult)

        emit(
            HomeItemConfig(
                flag = TOP_TV_FLAG,
                label = UiText.StringResource(R.string.top_tv_shows_recently),
                data = topTvShows.data!!.results
            )
        )

        val popularMovies = tmdbRepository.discoverFilms(
            mediaType = FilmType.MOVIE.type,
            page = 1
        )
        if (popularMovies is Resource.Failure)
            return@flow emit(errorResult)

        emit(
            HomeItemConfig(
                flag = POPULAR_MOVIE_FLAG,
                label = UiText.StringResource(R.string.popular_movies),
                data = popularMovies.data!!.results
            )
        )

        val popularTvShows = tmdbRepository.discoverFilms(
            mediaType = FilmType.TV_SHOW.type,
            page = 1
        )
        if (popularTvShows is Resource.Failure)
            return@flow emit(errorResult)

        emit(
            HomeItemConfig(
                flag = POPULAR_TV_FLAG,
                label = UiText.StringResource(R.string.popular_tv_shows),
                data = popularTvShows.data!!.results
            )
        )
    }

    override fun getWatchProvidersRowItems(count: Int): Flow<HomeItemConfig?> = flow {
        check(count > 0) { "Item count must be greater than 0" }

        // Get random network/company items
        val providersFound = mutableListOf<WatchProvider>()
        for (i in 0 until count) {
            val randomProvider = WatchProvider.values().random()

            val providerId = randomProvider.id
            val providerStringId = randomProvider.labelId

            val isProviderAlreadyAdded = providersFound.find { it.labelId == providerStringId } != null
            if(isProviderAlreadyAdded)
                break

            providersFound.add(randomProvider)
            val result = when(randomProvider.isCompany) {
                true -> tmdbRepository.discoverFilms(
                    mediaType = FilmType.MOVIE.type,
                    page = 1,
                    withCompanies = listOf(randomProvider.id),
                    sortBy = SortOptions.POPULARITY
                )
                false -> tmdbRepository.discoverFilms(
                    mediaType = FilmType.TV_SHOW.type,
                    page = 1,
                    withNetworks = listOf(randomProvider.id),
                    sortBy = SortOptions.POPULARITY
                )
            }
            if(result is Resource.Failure)
                return@flow emit(null)

            emit(
                HomeItemConfig(
                    flag = providerId.toString(),
                    label = UiText.StringResource(providerStringId),
                    data = result.data!!.results
                )
            )
        }
    }

    override fun getGenreRowItems(count: Int): Flow<HomeItemConfig?> = flow {
        check(count > 0) { "Item count must be greater than 0" }

        // Get random genre items
        val genresFound = mutableListOf<Genre>()
        for (i in 0 until count) {
            val randomGenre = GENRES_LIST.random()

            val isGenreAlreadyAdded = genresFound.find { it.name.equals(randomGenre.name, true) } != null
            if(isGenreAlreadyAdded)
                break

            genresFound.add(randomGenre)
            val genreTitle = randomGenre.name.toTitleCase()

            val result = tmdbRepository.discoverFilms(
                mediaType = FilmType.MOVIE.type,
                page = 1,
                withGenres = listOf(randomGenre),
                sortBy = SortOptions.POPULARITY
            )
            if(result is Resource.Failure)
                return@flow emit(null)

            emit(
                HomeItemConfig(
                    flag = genreTitle,
                    label = UiText.StringResource(randomGenre.labelId!!),
                    data = result.data!!.results
                )
            )
        }
    }

    override fun getBasedOnRowItems(count: Int): Flow<HomeItemConfig?> = flow {
        check(count > 0) { "Item count must be greater than 0" }

        val randomWatchedFilms = watchHistoryRepository.getRandomWatchHistoryItems(count)
        if(randomWatchedFilms.isNotEmpty()) {
            randomWatchedFilms.forEach { item ->
                emit(
                    HomeItemConfig(
                        flag = null,
                        label = UiText.StringValue("If you liked \"${item.film.title}\""),
                        data = item.film.recommendedTitles
                    )
                )
                delay(800L) // Delay on re-iteration to have a humanized home items list
            }
        }
    }
}