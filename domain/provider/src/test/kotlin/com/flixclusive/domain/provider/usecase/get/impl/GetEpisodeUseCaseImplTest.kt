package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.domain.provider.usecase.get.GetEpisodeUseCase
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class GetEpisodeUseCaseImplTest {
    private val tmdbMetadataRepository = mockk<TMDBMetadataRepository>()

    private lateinit var useCase: GetEpisodeUseCase

    @Before
    fun setUp() {
        useCase = GetEpisodeUseCaseImpl(
            tmdbMetadataRepository = tmdbMetadataRepository,
        )
    }

    @Test
    fun `invoke with existing episode should return episode`() = runTest {
        // Given
        val targetEpisode = FilmTestDefaults.getEpisode(
            number = 2,
            title = "Test Episode",
        )

        val tvShow = FilmTestDefaults.getTvShow(
            seasons = listOf(
                FilmTestDefaults.getSeason(
                    number = 1,
                    episodes = listOf(
                        FilmTestDefaults.getEpisode(number = 1),
                        targetEpisode,
                    ),
                ),
            ),
        )

        // When
        val result = useCase(tvShow, 1, 2)

        // Then
        expectThat(result).isEqualTo(targetEpisode)
    }

    @Test
    fun `invoke with non-existent episode should return null`() = runTest {
        // Given
        val tvShow = FilmTestDefaults.getTvShow(
            seasons = listOf(
                FilmTestDefaults.getSeason(
                    number = 1,
                    episodes = listOf(
                        FilmTestDefaults.getEpisode(number = 1),
                    ),
                ),
            ),
        )

        coEvery {
            tmdbMetadataRepository.getSeason(tvShow.tmdbId!!, any())
        } returns Resource.Failure("Not found")

        // When
        val result = useCase(tvShow, 1, 5)

        // Then
        expectThat(result).isNull()
    }

    @Test
    fun `invoke with TMDB show should fetch episode from repository`() = runTest {
        // Given
        val tmdbTvShow = FilmTestDefaults.getTvShow(
            providerId = DEFAULT_FILM_SOURCE_NAME,
            tmdbId = 1399,
            seasons = emptyList(),
        )
        val expectedEpisode = FilmTestDefaults.getEpisode(number = 1)
        val expectedSeason = FilmTestDefaults.getSeason(
            episodes = listOf(expectedEpisode),
        )

        coEvery {
            tmdbMetadataRepository.getSeason(1399, 1)
        } returns Resource.Success(expectedSeason)

        // When
        val result = useCase(tmdbTvShow, 1, 1)

        // Then
        expectThat(result).isEqualTo(expectedEpisode)
    }

    @Test
    fun `invoke with TMDB show repository failure should return null`() = runTest {
        // Given
        val tmdbTvShow = FilmTestDefaults.getTvShow(
            providerId = DEFAULT_FILM_SOURCE_NAME,
            tmdbId = 1399,
            seasons = emptyList(),
        )

        coEvery {
            tmdbMetadataRepository.getSeason(1399, 1)
        } returns Resource.Failure("Not found")

        // When
        val result = useCase(tmdbTvShow, 1, 1)

        // Then
        expectThat(result).isNull()
    }
}
