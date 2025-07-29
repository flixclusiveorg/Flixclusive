package com.flixclusive.domain.provider.fake

import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.tmdb.SortOptions
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TMDBCollection
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.Stream

class FakeTMDBRepository : TMDBRepository {
    private var shouldReturnError = false
    private var errorMessage = "TMDB Error"
    private var seasonsToReturn = mutableMapOf<Int, Season>()

    override suspend fun getSeason(
        id: Int,
        seasonNumber: Int,
    ): Resource<Season> {
        return if (shouldReturnError) {
            Resource.Failure(errorMessage)
        } else {
            val season = seasonsToReturn[seasonNumber] ?: createMockSeason(seasonNumber)
            Resource.Success(season)
        }
    }

    override suspend fun getWatchProviders(
        mediaType: String,
        id: Int,
    ): Resource<List<Stream>> {
        return if (shouldReturnError) {
            Resource.Failure("Watch providers failed")
        } else {
            Resource.Success(
                listOf(
                    Stream(
                        name = "Mock Provider",
                        url = "https://example.com/watch",
                        flags = setOf(
                            Flag.Trusted("Mock Provider", "https://example.com/logo.png"),
                        ),
                    ),
                ),
            )
        }
    }

    // Unused methods - provide minimal implementations
    override suspend fun getMovie(id: Int): Resource<Movie> = Resource.Failure("Not implemented")

    override suspend fun getTvShow(id: Int): Resource<TvShow> = Resource.Failure("Not implemented")

    override suspend fun getTrending(
        mediaType: String,
        timeWindow: String,
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>> = Resource.Failure("Not implemented")

    override suspend fun discoverFilms(
        mediaType: String,
        page: Int,
        withNetworks: List<Int>?,
        withCompanies: List<Int>?,
        withGenres: List<Genre>?,
        sortBy: SortOptions,
    ): Resource<SearchResponseData<FilmSearchItem>> = Resource.Failure("Not implemented")

    override suspend fun search(
        query: String,
        page: Int,
        filter: Int,
    ): Resource<SearchResponseData<FilmSearchItem>> = Resource.Failure("Not implemented")

    override suspend fun getLogo(
        mediaType: String,
        id: Int,
    ): Resource<String> = Resource.Failure("Not implemented")

    override suspend fun getPosterWithoutLogo(
        mediaType: String,
        id: Int,
    ): Resource<String> = Resource.Failure("Not implemented")

    override suspend fun getEpisode(
        id: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Resource<Episode?> = Resource.Failure("Not implemented")

    override suspend fun getCollection(id: Int): Resource<TMDBCollection> = Resource.Failure("Not implemented")

    override suspend fun paginateConfigItems(
        url: String,
        page: Int,
    ): Resource<SearchResponseData<FilmSearchItem>> = Resource.Failure("Not implemented")

    private fun createMockSeason(seasonNumber: Int): Season {
        val episodes = listOf(
            Episode(
                id = "1",
                number = 1,
                season = seasonNumber,
                title = "Episode 1",
                image = null,
                rating = 8.0,
                runtime = 45,
            ),
            Episode(
                id = "2",
                number = 2,
                season = seasonNumber,
                title = "Episode 2",
                image = null,
                rating = 8.2,
                runtime = 45,
            ),
        )

        return Season(
            number = seasonNumber,
            image = null,
            episodes = episodes,
        )
    }

    fun setShouldReturnError(
        shouldError: Boolean,
        message: String = "TMDB Error",
    ) {
        shouldReturnError = shouldError
        errorMessage = message
    }

    fun addMockSeason(
        seasonNumber: Int,
        episodes: List<Episode>,
    ) {
        val season = Season(
            number = seasonNumber,
            image = null,
            episodes = episodes,
        )
        seasonsToReturn[seasonNumber] = season
    }
}
