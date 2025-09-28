package com.flixclusive.domain.catalog.usecase.impl

import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.extensions.isFailure
import com.flixclusive.core.testing.extensions.isSuccess
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.data.tmdb.model.TMDBDiscoverCatalog
import com.flixclusive.data.tmdb.repository.TMDBDiscoverCatalogRepository
import com.flixclusive.data.tmdb.repository.TMDBFilmSearchItemsRepository
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.util.FilmType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class GetDiscoverCardsUseCaseImplTest {
    private lateinit var tmdbDiscoverCatalogRepository: TMDBDiscoverCatalogRepository
    private lateinit var tmdbFilmSearchItemsRepository: TMDBFilmSearchItemsRepository
    private lateinit var useCase: GetDiscoverCardsUseCaseImpl

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val logRule = LogRule()

    @Before
    fun setup() {
        tmdbDiscoverCatalogRepository = mockk()
        tmdbFilmSearchItemsRepository = mockk()
        useCase = GetDiscoverCardsUseCaseImpl(
            tmdbDiscoverCatalogRepository = tmdbDiscoverCatalogRepository,
            tmdbFilmSearchItemsRepository = tmdbFilmSearchItemsRepository,
        )
    }

    @Test
    fun `invoke returns success with all catalog types when repositories return data`() =
        runTest(testDispatcher) {
            // Arrange
            val tvNetworks = createTvNetworkCatalogs()
            val movieCompanies = createMovieCompanyCatalogs()
            val mediaTypes = createMediaTypeCatalogs()
            val genres = createGenreCatalogs()
            val searchResponse = createSearchResponseWithBackdropImages()

            coEvery { tmdbDiscoverCatalogRepository.getTvNetworks() } returns tvNetworks
            coEvery { tmdbDiscoverCatalogRepository.getMovieCompanies() } returns movieCompanies
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns mediaTypes
            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getGenres() } returns genres
            coEvery { tmdbFilmSearchItemsRepository.get(any(), any()) } returns searchResponse

            // Act
            val result = useCase.invoke()

            // Assert
            expectThat(result).isSuccess()
            expectThat(result.data?.all).isNotNull().hasSize(4)

            // Verify all catalogs have thumbnails assigned
            result.data!!.all.forEach { catalog ->
                expectThat(catalog.image).isNotNull()
            }

            // Verify repositories were called
            coVerify { tmdbDiscoverCatalogRepository.getTvNetworks() }
            coVerify { tmdbDiscoverCatalogRepository.getMovieCompanies() }
            coVerify { tmdbDiscoverCatalogRepository.getTv() }
            coVerify { tmdbDiscoverCatalogRepository.getMovies() }
            coVerify { tmdbDiscoverCatalogRepository.getGenres() }
        }

    @Test
    fun `invoke returns cached data on subsequent calls`() =
        runTest(testDispatcher) {
            // Arrange
            val tvNetworks = createTvNetworkCatalogs()
            val searchResponse = createSearchResponseWithBackdropImages()

            coEvery { tmdbDiscoverCatalogRepository.getTvNetworks() } returns tvNetworks
            coEvery { tmdbDiscoverCatalogRepository.getMovieCompanies() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getGenres() } returns emptyList()
            coEvery { tmdbFilmSearchItemsRepository.get(any(), any()) } returns searchResponse

            // Act - First call
            val firstResult = useCase.invoke()

            // Act - Second call
            val secondResult = useCase.invoke()

            // Assert
            expectThat(firstResult).isSuccess()
            expectThat(secondResult).isSuccess()
            expectThat(firstResult.data).isEqualTo(secondResult.data)

            // Verify repositories were only called once
            coVerify(exactly = 1) { tmdbDiscoverCatalogRepository.getTvNetworks() }
            coVerify(exactly = 1) { tmdbDiscoverCatalogRepository.getMovieCompanies() }
        }

    @Test
    fun `invoke handles repository exception and returns failure`() =
        runTest(testDispatcher) {
            // Arrange
            val exception = RuntimeException("Repository error")
            coEvery { tmdbDiscoverCatalogRepository.getTvNetworks() } throws exception

            // Act
            val result = useCase.invoke()

            // Assert
            expectThat(result).isFailure()
            expectThat(result.error).isNotNull()
        }

    @Test
    fun `getThumbnail returns null when no backdrop images available`() =
        runTest(testDispatcher) {
            // Arrange
            val catalog = createTvNetworkCatalogs().first()
            val searchResponseWithoutBackdrops = createSearchResponseWithoutBackdropImages()

            coEvery { tmdbDiscoverCatalogRepository.getTvNetworks() } returns listOf(catalog)
            coEvery { tmdbDiscoverCatalogRepository.getMovieCompanies() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getGenres() } returns emptyList()
            coEvery { tmdbFilmSearchItemsRepository.get(any(), any()) } returns searchResponseWithoutBackdrops

            // Act
            val result = useCase.invoke()

            // Assert
            expectThat(result).isSuccess()
            expectThat(
                result.data!!
                    .all
                    .first()
                    .image,
            ).isNull()
        }

    @Test
    fun `getThumbnail tries multiple pages when first page has no backdrop images`() =
        runTest(testDispatcher) {
            // Arrange
            val catalog = createTvNetworkCatalogs().first()
            val emptyResponse = createSearchResponseWithoutBackdropImages()
            val responseWithBackdrop = createSearchResponseWithBackdropImages()

            coEvery { tmdbDiscoverCatalogRepository.getTvNetworks() } returns listOf(catalog)
            coEvery { tmdbDiscoverCatalogRepository.getMovieCompanies() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getGenres() } returns emptyList()
            coEvery { tmdbFilmSearchItemsRepository.get(catalog.url, 1) } returns emptyResponse
            coEvery { tmdbFilmSearchItemsRepository.get(catalog.url, 2) } returns responseWithBackdrop

            // Act
            val result = useCase.invoke()

            // Assert
            expectThat(result).isSuccess()
            expectThat(
                result.data!!
                    .all
                    .first()
                    .image,
            ).isNotNull()

            // Verify multiple pages were tried
            coVerify { tmdbFilmSearchItemsRepository.get(catalog.url, 1) }
            coVerify { tmdbFilmSearchItemsRepository.get(catalog.url, 2) }
        }

    @Test
    fun `getThumbnail handles film search repository failure gracefully`() =
        runTest(testDispatcher) {
            // Arrange
            val catalog = createTvNetworkCatalogs().first()

            coEvery { tmdbDiscoverCatalogRepository.getTvNetworks() } returns listOf(catalog)
            coEvery { tmdbDiscoverCatalogRepository.getMovieCompanies() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getGenres() } returns emptyList()
            coEvery { tmdbFilmSearchItemsRepository.get(any(), any()) } returns Resource.Failure("Network error")

            // Act
            val result = useCase.invoke()

            // Assert
            expectThat(result).isSuccess()
            expectThat(
                result.data!!
                    .all
                    .first()
                    .image,
            ).isNull()
        }

    @Test
    fun `catalogs are sorted by name within each category`() =
        runTest(testDispatcher) {
            // Arrange
            val unsortedTvNetworks = listOf(
                createTMDBDiscoverCatalog(name = "Z Network", id = 1),
                createTMDBDiscoverCatalog(name = "A Network", id = 2),
            )
            val searchResponse = createSearchResponseWithBackdropImages()

            coEvery { tmdbDiscoverCatalogRepository.getTvNetworks() } returns unsortedTvNetworks
            coEvery { tmdbDiscoverCatalogRepository.getMovieCompanies() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getTv() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getMovies() } returns emptyList()
            coEvery { tmdbDiscoverCatalogRepository.getGenres() } returns emptyList()
            coEvery { tmdbFilmSearchItemsRepository.get(any(), any()) } returns searchResponse

            // Act
            val result = useCase.invoke()

            // Assert
            expectThat(result).isSuccess()
            val sortedNames = result.data!!.all.map { it.name }
            expectThat(sortedNames).isEqualTo(listOf("A Network", "Z Network"))
        }

    private fun createTvNetworkCatalogs(): List<TMDBDiscoverCatalog> {
        return listOf(
            createTMDBDiscoverCatalog(name = "Netflix", id = 1),
        )
    }

    private fun createMovieCompanyCatalogs(): List<TMDBDiscoverCatalog> {
        return listOf(
            createTMDBDiscoverCatalog(name = "Warner Bros", id = 2),
        )
    }

    private fun createMediaTypeCatalogs(): List<TMDBDiscoverCatalog> {
        return listOf(
            createTMDBDiscoverCatalog(name = "Popular TV", id = 3, mediaType = FilmType.TV_SHOW.type),
        )
    }

    private fun createGenreCatalogs(): List<TMDBDiscoverCatalog> {
        return listOf(
            createTMDBDiscoverCatalog(name = "Action", id = 4),
        )
    }

    private fun createTMDBDiscoverCatalog(
        name: String,
        id: Int,
        mediaType: String = FilmType.MOVIE.type,
        url: String = "https://api.themoviedb.org/3/discover/movie",
    ): TMDBDiscoverCatalog {
        return TMDBDiscoverCatalog(
            id = id,
            name = name,
            mediaType = mediaType,
            url = url,
            image = null,
        )
    }

    private fun createSearchResponseWithBackdropImages(): Resource<SearchResponseData<FilmSearchItem>> {
        val filmWithBackdrop = FilmTestDefaults.getFilmSearchItem(
            backdropImage = "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
        )
        val searchResponse = SearchResponseData(
            page = 1,
            results = listOf(filmWithBackdrop),
            hasNextPage = false,
            totalPages = 1,
        )
        return Resource.Success(searchResponse)
    }

    private fun createSearchResponseWithoutBackdropImages(): Resource<SearchResponseData<FilmSearchItem>> {
        val searchResponse = SearchResponseData<FilmSearchItem>(
            page = 1,
            results = emptyList(), // Empty list instead of items with null backdrops
            hasNextPage = false,
            totalPages = 1,
        )
        return Resource.Success(searchResponse)
    }
}
