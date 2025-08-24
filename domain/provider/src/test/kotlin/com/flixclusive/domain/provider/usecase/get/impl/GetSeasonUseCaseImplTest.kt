package com.flixclusive.domain.provider.usecase.get.impl

import app.cash.turbine.test
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.domain.provider.usecase.get.GetSeasonUseCase
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Season
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class GetSeasonUseCaseImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private val tmdbMetadataRepository = mockk<TMDBMetadataRepository>()

    private lateinit var useCase: GetSeasonUseCase

    @Before
    fun setUp() {
        useCase = GetSeasonUseCaseImpl(
            tmdbMetadataRepository = tmdbMetadataRepository,
        )
    }

    @Test
    fun `invoke with TMDB TV show should return season metadata from mocked repository`() =
        runTest(testDispatcher) {
            // Given
            val tmdbTvShow = FilmTestDefaults.getTvShow(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                tmdbId = 1399,
                seasons = emptyList(),
            )
            val seasonNumber = 1
            val expectedSeason = FilmTestDefaults.getSeason(
                number = seasonNumber,
                name = "Season 1",
            )

            coEvery {
                tmdbMetadataRepository.getSeason(1399, seasonNumber)
            } returns Resource.Success(expectedSeason)

            // When
            useCase(tmdbTvShow, seasonNumber).test {
                // Then
                val loadingState = awaitItem()
                expectThat(loadingState).isA<Resource.Loading>()

                val successState = awaitItem()
                expectThat(successState).isA<Resource.Success<Season>>()

                val season = successState.data!!
                expectThat(season.number).isEqualTo(seasonNumber)
                expectThat(season.name).isEqualTo("Season 1")
                expectThat(season.episodes).isA<List<*>>()

                awaitComplete()
            }
        }

    @Test
    fun `invoke with TMDB TV show breaking bad should return season metadata from mocked repository`() =
        runTest(testDispatcher) {
            // Given
            val tmdbTvShow = FilmTestDefaults.getTvShow(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                tmdbId = 1396,
                title = "Breaking Bad",
                seasons = emptyList(),
            )
            val seasonNumber = 1
            val expectedSeason = FilmTestDefaults.getSeason(
                number = seasonNumber,
                name = "Season 1",
            )

            coEvery {
                tmdbMetadataRepository.getSeason(1396, seasonNumber)
            } returns Resource.Success(expectedSeason)

            // When
            useCase(tmdbTvShow, seasonNumber).test {
                // Then
                val loadingState = awaitItem()
                expectThat(loadingState).isA<Resource.Loading>()

                val successState = awaitItem()
                expectThat(successState).isA<Resource.Success<Season>>()

                val season = successState.data!!
                expectThat(season.number).isEqualTo(seasonNumber)
                expectThat(season.episodes).isA<List<*>>()

                awaitComplete()
            }
        }

    @Test
    fun `invoke with invalid TMDB season number should return failure`() =
        runTest(testDispatcher) {
            // Given
            val tmdbTvShow = FilmTestDefaults.getTvShow(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                tmdbId = 1399,
                seasons = emptyList(),
            )
            val invalidSeasonNumber = 999

            coEvery {
                tmdbMetadataRepository.getSeason(1399, invalidSeasonNumber)
            } returns Resource.Failure("Season not found")

            // When
            useCase(tmdbTvShow, invalidSeasonNumber).test {
                // Then
                val loadingState = awaitItem()
                expectThat(loadingState).isA<Resource.Loading>()

                val failureState = awaitItem()
                expectThat(failureState).isA<Resource.Failure>()
                expectThat(failureState.error).isNotNull()

                awaitComplete()
            }
        }

    @Test
    fun `invoke with invalid TMDB show ID should return failure`() =
        runTest(testDispatcher) {
            // Given
            val tmdbTvShow = FilmTestDefaults.getTvShow(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                tmdbId = 999999999,
                seasons = emptyList(),
            )
            val seasonNumber = 1

            coEvery {
                tmdbMetadataRepository.getSeason(999999999, seasonNumber)
            } returns Resource.Failure("Show not found")

            // When
            useCase(tmdbTvShow, seasonNumber).test {
                // Then
                val loadingState = awaitItem()
                expectThat(loadingState).isA<Resource.Loading>()

                val failureState = awaitItem()
                expectThat(failureState).isA<Resource.Failure>()
                expectThat(failureState.error).isNotNull()

                awaitComplete()
            }
        }

    @Test
    fun `invoke with non-default provider should return season from tv show data`() =
        runTest(testDispatcher) {
            // Given
            val customProviderId = "test-provider"
            val seasonNumber = 2
            val testSeason = FilmTestDefaults.getSeason(
                number = seasonNumber,
                name = "Season 2",
                overview = "Test season overview",
            )

            val customProviderTvShow = FilmTestDefaults.getTvShow(
                providerId = customProviderId,
                seasons = listOf(
                    FilmTestDefaults.getSeason(number = 1),
                    testSeason,
                    FilmTestDefaults.getSeason(number = 3),
                ),
            )

            // When
            useCase(customProviderTvShow, seasonNumber).test {
                // Then
                val loadingState = awaitItem()
                expectThat(loadingState).isA<Resource.Loading>()

                val successState = awaitItem()
                expectThat(successState).isA<Resource.Success<Season>>()

                val season = successState.data!!
                expectThat(season.number).isEqualTo(seasonNumber)
                expectThat(season.name).isEqualTo("Season 2")
                expectThat(season.overview).isEqualTo("Test season overview")

                awaitComplete()
            }
        }

    @Test
    fun `invoke with non-default provider but season not found should return failure`() =
        runTest(testDispatcher) {
            // Given
            val customProviderId = "test-provider"
            val nonExistentSeasonNumber = 5

            val customProviderTvShow = FilmTestDefaults.getTvShow(
                providerId = customProviderId,
                seasons = listOf(
                    FilmTestDefaults.getSeason(number = 1),
                    FilmTestDefaults.getSeason(number = 2),
                    FilmTestDefaults.getSeason(number = 3),
                ),
            )

            // When
            useCase(customProviderTvShow, nonExistentSeasonNumber).test {
                // Then
                val loadingState = awaitItem()
                expectThat(loadingState).isA<Resource.Loading>()

                val failureState = awaitItem()
                expectThat(failureState).isA<Resource.Failure>()
                expectThat(failureState.error).isNotNull()

                awaitComplete()
            }
        }

    @Test
    fun `invoke with unknown provider should return failure`() =
        runTest(testDispatcher) {
            // Given - Create a TV show that doesn't match TMDB or non-default provider conditions
            val unknownProviderTvShow = TvShow(
                id = "unknown-show",
                title = "Unknown Show",
                providerId = "unknown-provider",
                posterImage = null,
                homePage = null,
                tmdbId = null, // No TMDB ID
                seasons = emptyList(), // Empty seasons
            )
            val seasonNumber = 1

            // When
            useCase(unknownProviderTvShow, seasonNumber).test {
                // Then
                val loadingState = awaitItem()
                expectThat(loadingState).isA<Resource.Loading>()

                val failureState = awaitItem()
                expectThat(failureState).isA<Resource.Failure>()
                expectThat(failureState.error).isNotNull()

                awaitComplete()
            }
        }

    @Test
    fun `invoke with multiple seasons in custom provider should return correct season`() =
        runTest(testDispatcher) {
            // Given
            val customProviderId = "multi-season-provider"
            val targetSeasonNumber = 3
            val season1 = FilmTestDefaults.getSeason(number = 1, name = "First Season")
            val season2 = FilmTestDefaults.getSeason(number = 2, name = "Second Season")
            val targetSeason = FilmTestDefaults.getSeason(number = targetSeasonNumber, name = "Target Season")
            val season4 = FilmTestDefaults.getSeason(number = 4, name = "Fourth Season")

            val customProviderTvShow = FilmTestDefaults.getTvShow(
                providerId = customProviderId,
                seasons = listOf(season1, season2, targetSeason, season4),
            )

            // When
            useCase(customProviderTvShow, targetSeasonNumber).test {
                // Then
                val loadingState = awaitItem()
                expectThat(loadingState).isA<Resource.Loading>()

                val successState = awaitItem()
                expectThat(successState).isA<Resource.Success<Season>>()

                val season = successState.data!!
                expectThat(season.number).isEqualTo(targetSeasonNumber)
                expectThat(season.name).isEqualTo("Target Season")

                awaitComplete()
            }
        }

    @Test
    fun `invoke with TMDB show without tmdb ID should return failure`() =
        runTest(testDispatcher) {
            // Given
            val tmdbTvShow = FilmTestDefaults.getTvShow(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                tmdbId = null,
                seasons = emptyList(),
            )
            val seasonNumber = 1

            // When
            useCase(tmdbTvShow, seasonNumber).test {
                // Then
                val loadingState = awaitItem()
                expectThat(loadingState).isA<Resource.Loading>()

                val failureState = awaitItem()
                expectThat(failureState).isA<Resource.Failure>()
                expectThat(failureState.error).isNotNull()

                awaitComplete()
            }
        }
}
