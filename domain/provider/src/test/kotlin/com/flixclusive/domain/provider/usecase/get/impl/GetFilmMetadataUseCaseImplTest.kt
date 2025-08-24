package com.flixclusive.domain.provider.usecase.get.impl

import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.extensions.isFailure
import com.flixclusive.core.testing.extensions.isSuccess
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.provider.ProviderApi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class GetFilmMetadataUseCaseImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private val appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
    private val tmdbMetadataRepository = mockk<TMDBMetadataRepository>()
    private val providerApiRepository = mockk<ProviderApiRepository>()
    private val mockProviderApi = mockk<ProviderApi>()

    private lateinit var useCase: GetFilmMetadataUseCase

    @get:Rule
    val logRule = LogRule()

    @Before
    fun setUp() {
        useCase = GetFilmMetadataUseCaseImpl(
            tmdbMetadataRepository = tmdbMetadataRepository,
            providerApiRepository = providerApiRepository,
            appDispatchers = appDispatchers,
        )
    }

    @Test
    fun `invoke with custom provider film should return metadata from provider api`() =
        runTest(testDispatcher) {
            // Given
            val customProviderId = "test-provider"
            val customId = "custom-film-id"
            val testFilm = FilmTestDefaults.getFilmSearchItem(
                id = customId,
                providerId = customProviderId,
                filmType = FilmType.MOVIE,
            )
            val expectedMetadata = FilmTestDefaults.getMovie(providerId = customProviderId)

            every { providerApiRepository.getApi(customProviderId) } returns mockProviderApi
            coEvery { mockProviderApi.getMetadata(testFilm) } returns expectedMetadata

            // When
            val result = useCase(testFilm)

            // Then
            expectThat(result).isSuccess()
            expectThat(result.data).isNotNull().isEqualTo(expectedMetadata)
        }

    @Test
    fun `invoke with custom provider film should return error when provider api fails`() =
        runTest(testDispatcher) {
            // Given
            val customProviderId = "test-provider"
            val customId = "custom-film-id"
            val testFilm = FilmTestDefaults.getFilmSearchItem(
                id = customId,
                providerId = customProviderId,
                filmType = FilmType.MOVIE,
            )
            val exception = RuntimeException("Network error")

            every { providerApiRepository.getApi(customProviderId) } returns mockProviderApi
            coEvery { mockProviderApi.getMetadata(testFilm) } throws exception

            // When
            val result = useCase(testFilm)

            // Then
            expectThat(result).isFailure()
            expectThat(result.error).isNotNull()
        }

    @Test
    fun `invoke with custom provider film should return error when provider api not found`() =
        runTest(testDispatcher) {
            // Given
            val customProviderId = "non-existent-provider"
            val customId = "custom-film-id"
            val testFilm = FilmTestDefaults.getFilmSearchItem(
                id = customId,
                providerId = customProviderId,
                filmType = FilmType.MOVIE,
            )

            every { providerApiRepository.getApi(customProviderId) } returns null

            // When
            val result = useCase(testFilm)

            // Then
            expectThat(result).isFailure()
            expectThat(result.error).isNotNull()
        }

    @Test
    fun `invoke with TMDB movie should return movie metadata from TMDB repository`() =
        runTest(testDispatcher) {
            // Given
            val tmdbId = 238
            val testFilm = FilmTestDefaults.getFilmSearchItem(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                filmType = FilmType.MOVIE,
                tmdbId = tmdbId,
            )
            val expectedMovie = FilmTestDefaults.getMovie(tmdbId = tmdbId)
            val expectedResult = Resource.Success(expectedMovie)

            coEvery { tmdbMetadataRepository.getMovie(tmdbId) } returns expectedResult

            // When
            val result = useCase(testFilm)

            // Then
            expectThat(result).isSuccess()
            expectThat(result.data).isNotNull().isA<Movie>()
            expectThat(result.data).isEqualTo(expectedMovie)
        }

    @Test
    fun `invoke with TMDB TV show should return TV show metadata from TMDB repository`() =
        runTest(testDispatcher) {
            // Given
            val tmdbId = 1399
            val testFilm = FilmTestDefaults.getFilmSearchItem(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                filmType = FilmType.TV_SHOW,
                tmdbId = tmdbId,
            )
            val expectedTvShow = FilmTestDefaults.getTvShow(tmdbId = tmdbId)
            val expectedResult = Resource.Success(expectedTvShow)

            coEvery { tmdbMetadataRepository.getTvShow(tmdbId) } returns expectedResult

            // When
            val result = useCase(testFilm)

            // Then
            expectThat(result).isSuccess()
            expectThat(result.data).isNotNull().isA<TvShow>()
            expectThat(result.data).isEqualTo(expectedTvShow)
        }

    @Test
    fun `invoke with TMDB movie should return error when TMDB repository fails`() =
        runTest(testDispatcher) {
            // Given
            val tmdbId = 238
            val testFilm = FilmTestDefaults.getFilmSearchItem(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                filmType = FilmType.MOVIE,
                tmdbId = tmdbId,
            )
            val expectedResult = Resource.Failure("TMDB error")

            coEvery { tmdbMetadataRepository.getMovie(tmdbId) } returns expectedResult

            // When
            val result = useCase(testFilm)

            // Then
            expectThat(result).isFailure()
            expectThat(result.error).isNotNull()
        }

    @Test
    fun `invoke with TMDB TV show should return error when TMDB repository fails`() =
        runTest(testDispatcher) {
            // Given
            val tmdbId = 1399
            val testFilm = FilmTestDefaults.getFilmSearchItem(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                filmType = FilmType.TV_SHOW,
                tmdbId = tmdbId,
            )
            val expectedResult = Resource.Failure("TMDB error")

            coEvery { tmdbMetadataRepository.getTvShow(tmdbId) } returns expectedResult

            // When
            val result = useCase(testFilm)

            // Then
            expectThat(result).isFailure()
            expectThat(result.error).isNotNull()
        }

    @Test
    fun `invoke with non-TMDB film without provider should return film not found error`() =
        runTest(testDispatcher) {
            // Given
            val testFilm = FilmTestDefaults.getFilmSearchItem(
                providerId = "unknown-provider",
                filmType = FilmType.MOVIE,
                tmdbId = null,
            )

            // When
            val result = useCase(testFilm)

            // Then
            expectThat(result).isFailure()
            expectThat(result.error).isNotNull()
        }

    @Test
    fun `invoke with TMDB film without tmdbId should return film not found error`() =
        runTest(testDispatcher) {
            // Given
            val testFilm = FilmTestDefaults.getFilmSearchItem(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                filmType = FilmType.MOVIE,
                tmdbId = null,
            )

            // When
            val result = useCase(testFilm)

            // Then
            expectThat(result).isFailure()
            expectThat(result.error).isNotNull()
        }
}
