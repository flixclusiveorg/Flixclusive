package com.flixclusive.domain.catalog.usecase.impl

import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.data.tmdb.model.TMDBDiscoverCatalog
import com.flixclusive.data.tmdb.repository.TMDBDiscoverCatalogRepository
import com.flixclusive.data.tmdb.repository.TMDBFilmSearchItemsRepository
import com.flixclusive.data.tmdb.repository.TMDBMetadataRepository
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.util.FilmType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class GetHomeHeaderUseCaseImplTest {
    private lateinit var tmdbMetadataRepository: TMDBMetadataRepository
    private lateinit var tmdbFilmSearchItemsRepository: TMDBFilmSearchItemsRepository
    private lateinit var tmdbDiscoverCatalogRepository: TMDBDiscoverCatalogRepository
    private lateinit var getHomeHeaderUseCase: GetHomeHeaderUseCaseImpl

    @Before
    fun setUp() {
        tmdbMetadataRepository = mockk()
        tmdbFilmSearchItemsRepository = mockk()
        tmdbDiscoverCatalogRepository = mockk()

        getHomeHeaderUseCase = GetHomeHeaderUseCaseImpl(
            tmdbMetadataRepository = tmdbMetadataRepository,
            tmdbFilmSearchItemsRepository = tmdbFilmSearchItemsRepository,
            tmdbDiscoverCatalogRepository = tmdbDiscoverCatalogRepository,
        )
    }

    @Test
    fun `should return success with movie metadata when popular movie is found`() =
        runTest {
            val mockCatalog = mockk<TMDBDiscoverCatalog> {
                coEvery { url } returns "test-url"
            }
            val popularMovie = FilmTestDefaults.getFilmSearchItem(
                title = "Popular Movie",
                filmType = FilmType.MOVIE,
                tmdbId = 123,
                voteCount = 500,
            )
            val movieMetadata = FilmTestDefaults.getMovie(
                id = "123",
                tmdbId = 123,
                title = "Popular Movie",
                genres = listOf(Genre(1, "Action")),
            )
            val enhancedMovieMetadata = movieMetadata.copy(
                genres = movieMetadata.genres + movieMetadata.genres,
            )

            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns listOf(mockCatalog)
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns listOf(mockCatalog)
            coEvery {
                tmdbFilmSearchItemsRepository.get(url = "test-url", page = 1)
            } returns Resource.Success(
                SearchResponseData(
                    page = 1,
                    results = listOf(popularMovie),
                    totalPages = 1,
                ),
            )
            coEvery { tmdbMetadataRepository.getMovie(123) } returns Resource.Success(movieMetadata)

            val result = getHomeHeaderUseCase()

            expectThat(result).isA<Resource.Success<Movie>>()
            expectThat((result as Resource.Success).data).isEqualTo(enhancedMovieMetadata)
            coVerify { tmdbMetadataRepository.getMovie(123) }
        }

    @Test
    fun `should return success with tv show metadata when popular tv show is found`() =
        runTest {
            val mockCatalog = mockk<TMDBDiscoverCatalog> {
                coEvery { url } returns "test-url"
            }
            val popularTvShow = FilmTestDefaults.getFilmSearchItem(
                title = "Popular TV Show",
                filmType = FilmType.TV_SHOW,
                tmdbId = 456,
                voteCount = 400,
            )
            val tvShowMetadata = FilmTestDefaults.getTvShow(
                tmdbId = 456,
                title = "Popular TV Show",
                genres = listOf(Genre(2, "Drama")),
            )
            val enhancedTvShowMetadata = tvShowMetadata.copy(
                genres = tvShowMetadata.genres + tvShowMetadata.genres,
            )

            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns listOf(mockCatalog)
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns listOf(mockCatalog)
            coEvery {
                tmdbFilmSearchItemsRepository.get(url = "test-url", page = 1)
            } returns Resource.Success(
                SearchResponseData(
                    page = 1,
                    results = listOf(popularTvShow),
                    totalPages = 1,
                ),
            )
            coEvery { tmdbMetadataRepository.getTvShow(456) } returns Resource.Success(tvShowMetadata)

            val result = getHomeHeaderUseCase()

            expectThat(result).isA<Resource.Success<TvShow>>()
            expectThat((result as Resource.Success).data).isEqualTo(enhancedTvShowMetadata)
            coVerify { tmdbMetadataRepository.getTvShow(456) }
        }

    @Test
    fun `should skip unpopular movies with low vote count`() =
        runTest {
            val mockCatalog = mockk<TMDBDiscoverCatalog> {
                coEvery { url } returns "test-url"
            }
            val unpopularMovie = FilmTestDefaults.getFilmSearchItem(
                title = "Unpopular Movie",
                filmType = FilmType.MOVIE,
                tmdbId = 123,
                voteCount = 100,
            )
            val popularMovie = FilmTestDefaults.getFilmSearchItem(
                title = "Popular Movie",
                filmType = FilmType.MOVIE,
                tmdbId = 456,
                voteCount = 300,
            )
            val movieMetadata = FilmTestDefaults.getMovie(
                tmdbId = 456,
                title = "Popular Movie",
                genres = listOf(Genre(1, "Action")),
            )

            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns listOf(mockCatalog)
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns listOf(mockCatalog)
            coEvery {
                tmdbFilmSearchItemsRepository.get(url = "test-url", page = 1)
            } returns Resource.Success(
                SearchResponseData(
                    page = 1,
                    results = listOf(unpopularMovie, popularMovie),
                    totalPages = 1,
                ),
            )
            coEvery { tmdbMetadataRepository.getMovie(456) } returns Resource.Success(movieMetadata)

            val result = getHomeHeaderUseCase()

            expectThat(result).isA<Resource.Success<Movie>>()
            coVerify(exactly = 0) { tmdbMetadataRepository.getMovie(123) }
            coVerify { tmdbMetadataRepository.getMovie(456) }
        }

    @Test
    fun `should return failure when no suitable header item is found after max retries`() =
        runTest {
            val mockCatalog = mockk<TMDBDiscoverCatalog> {
                coEvery { url } returns "test-url"
            }
            val unpopularMovie = FilmTestDefaults.getFilmSearchItem(
                title = "Unpopular Movie",
                filmType = FilmType.MOVIE,
                tmdbId = 123,
                voteCount = 100,
            )

            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns listOf(mockCatalog)
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns listOf(mockCatalog)
            coEvery {
                tmdbFilmSearchItemsRepository.get(url = "test-url", page = 1)
            } returns Resource.Success(
                SearchResponseData(
                    page = 1,
                    results = listOf(unpopularMovie),
                    totalPages = 1,
                ),
            )

            val result = getHomeHeaderUseCase()

            expectThat(result).isA<Resource.Failure>()
        }

    @Test
    fun `should handle empty search results gracefully`() =
        runTest {
            val mockCatalog = mockk<TMDBDiscoverCatalog> {
                coEvery { url } returns "test-url"
            }

            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns listOf(mockCatalog)
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns listOf(mockCatalog)
            coEvery {
                tmdbFilmSearchItemsRepository.get(url = "test-url", page = 1)
            } returns Resource.Success(
                SearchResponseData(
                    page = 1,
                    results = emptyList(),
                    totalPages = 1,
                ),
            )

            val result = getHomeHeaderUseCase()

            expectThat(result).isA<Resource.Failure>()
        }

    @Test
    fun `should handle failed search requests gracefully`() =
        runTest {
            val mockCatalog = mockk<TMDBDiscoverCatalog> {
                coEvery { url } returns "test-url"
            }

            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns listOf(mockCatalog)
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns listOf(mockCatalog)
            coEvery {
                tmdbFilmSearchItemsRepository.get(url = "test-url", page = 1)
            } returns Resource.Failure("Network error")

            val result = getHomeHeaderUseCase()

            expectThat(result).isA<Resource.Failure>()
        }

    @Test
    fun `should handle metadata fetch failure gracefully`() =
        runTest {
            val mockCatalog = mockk<TMDBDiscoverCatalog> {
                coEvery { url } returns "test-url"
            }
            val popularMovie = FilmTestDefaults.getFilmSearchItem(
                title = "Popular Movie",
                filmType = FilmType.MOVIE,
                tmdbId = 123,
                voteCount = 500,
            )

            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns listOf(mockCatalog)
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns listOf(mockCatalog)
            coEvery {
                tmdbFilmSearchItemsRepository.get(url = "test-url", page = 1)
            } returns Resource.Success(
                SearchResponseData(
                    page = 1,
                    results = listOf(popularMovie),
                    totalPages = 1,
                ),
            )
            coEvery { tmdbMetadataRepository.getMovie(123) } returns Resource.Failure("Metadata not found")

            val result = getHomeHeaderUseCase()

            expectThat(result).isA<Resource.Failure>()
        }

    @Test
    fun `should handle film without tmdb id gracefully`() =
        runTest {
            val mockCatalog = mockk<TMDBDiscoverCatalog> {
                coEvery { url } returns "test-url"
            }
            val filmWithoutTmdbId = FilmTestDefaults.getFilmSearchItem(
                title = "Film Without TMDB ID",
                filmType = FilmType.MOVIE,
                tmdbId = null,
                voteCount = 500,
            )

            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns listOf(mockCatalog)
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns listOf(mockCatalog)
            coEvery {
                tmdbFilmSearchItemsRepository.get(url = "test-url", page = 1)
            } returns Resource.Success(
                SearchResponseData(
                    page = 1,
                    results = listOf(filmWithoutTmdbId),
                    totalPages = 1,
                ),
            )

            val result = getHomeHeaderUseCase()

            expectThat(result).isA<Resource.Failure>()
        }
}
