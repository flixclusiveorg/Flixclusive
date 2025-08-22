package com.flixclusive.data.tmdb.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.extensions.isFailure
import com.flixclusive.core.testing.tmdb.TMDBTestDefaults
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull

class TMDBMetadataRepositoryImplTest {
    private lateinit var tmdbApiService: TMDBApiService
    private lateinit var appDispatchers: AppDispatchers
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TMDBMetadataRepositoryImpl

    @get:Rule
    val logRule = LogRule()

    @Before
    fun setup() {
        tmdbApiService = TMDBTestDefaults.createTMDBApiService()
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        repository = TMDBMetadataRepositoryImpl(
            tmdbApiService,
            appDispatchers,
        )
    }

    @Test
    fun `getMovie returns success with real movie data`() =
        runTest(testDispatcher) {
            val movieId = 550 // Fight Club - a well-known movie ID

            val result = repository.getMovie(movieId)

            expectThat(result).isA<Resource.Success<Movie>>()
            val movie = (result as Resource.Success).data
            expectThat(movie).isNotNull()
            expectThat(movie!!.tmdbId).isEqualTo(movieId)
            expectThat(movie.title).isNotEmpty()
            expectThat(movie.overview).isNotNull()
        }

    @Test
    fun `getTvShow returns success with real TV show data`() =
        runTest(testDispatcher) {
            val tvShowId = 1399 // Game of Thrones - a well-known TV show ID

            val result = repository.getTvShow(tvShowId)

            expectThat(result).isA<Resource.Success<TvShow>>()
            val tvShow = (result as Resource.Success).data
            expectThat(tvShow).isNotNull()
            expectThat(tvShow!!.tmdbId).isEqualTo(tvShowId)
            expectThat(tvShow.overview).isNotNull()
            expectThat(tvShow.seasons).isNotEmpty()
        }

    @Test
    fun `getSeason returns success with real season data`() =
        runTest(testDispatcher) {
            val tvShowId = 1399 // Game of Thrones
            val seasonNumber = 1

            val result = repository.getSeason(tvShowId, seasonNumber)

            expectThat(result).isA<Resource.Success<Season>>()
            val season = (result as Resource.Success).data
            expectThat(season).isNotNull()
            expectThat(season!!.number).isEqualTo(seasonNumber)
            expectThat(season.episodes).isNotEmpty()
        }

    @Test
    fun `getEpisode returns success with found episode from real data`() =
        runTest(testDispatcher) {
            val tvShowId = 1399 // Game of Thrones
            val seasonNumber = 1
            val episodeNumber = 1

            val result = repository.getEpisode(tvShowId, seasonNumber, episodeNumber)

            expectThat(result).isA<Resource.Success<Episode?>>()
            val episode = (result as Resource.Success).data
            expectThat(episode).isNotNull()
            expectThat(episode!!.season).isEqualTo(seasonNumber)
            expectThat(episode.number).isEqualTo(episodeNumber)
        }

    @Test
    fun `getEpisode returns success with null when episode not found`() =
        runTest(testDispatcher) {
            val tvShowId = 1399 // Game of Thrones
            val seasonNumber = 1
            val episodeNumber = 999 // Non-existent episode

            val result = repository.getEpisode(tvShowId, seasonNumber, episodeNumber)

            expectThat(result).isA<Resource.Success<Episode?>>()
            expectThat((result as Resource.Success).data).isEqualTo(null)
        }

    @Test
    fun `getMovie returns failure for non-existent movie`() =
        runTest(testDispatcher) {
            val invalidMovieId = 999999999

            val result = repository.getMovie(invalidMovieId)

            expectThat(result).isFailure()
        }

    @Test
    fun `getTvShow returns failure for non-existent TV show`() =
        runTest(testDispatcher) {
            val invalidTvShowId = 999999999

            val result = repository.getTvShow(invalidTvShowId)

            expectThat(result).isFailure()
        }

    @Test
    fun `getMovie with popular movie returns comprehensive data`() =
        runTest(testDispatcher) {
            val movieId = 27205 // Inception - comprehensive data

            val result = repository.getMovie(movieId)

            expectThat(result).isA<Resource.Success<Movie>>()
            val movie = (result as Resource.Success).data
            expectThat(movie).isNotNull()
            expectThat(movie!!.title).isNotEmpty()
            expectThat(movie.releaseDate).isNotNull()
            expectThat(movie.rating).isNotNull()
            expectThat(movie.rating!!).isGreaterThan(0.0)
        }

    @Test
    fun `getTvShow with seasons filters out zero seasons`() =
        runTest(testDispatcher) {
            val tvShowId = 1399 // Game of Thrones

            val result = repository.getTvShow(tvShowId)

            expectThat(result).isA<Resource.Success<TvShow>>()
            val tvShow = (result as Resource.Success).data
            expectThat(tvShow).isNotNull()

            val hasZeroSeason = tvShow!!.seasons.any { it.number == 0 }
            expectThat(hasZeroSeason).isEqualTo(false)
        }
}
