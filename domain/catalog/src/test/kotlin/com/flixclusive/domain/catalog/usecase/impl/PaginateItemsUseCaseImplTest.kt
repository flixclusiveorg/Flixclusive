package com.flixclusive.domain.catalog.usecase.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.extensions.isFailure
import com.flixclusive.core.testing.extensions.isSuccess
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.tmdb.repository.TMDBFilmSearchItemsRepository
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.provider.ProviderApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PaginateItemsUseCaseImplTest {
    private lateinit var tmdbFilmSearchItemsRepository: TMDBFilmSearchItemsRepository
    private lateinit var providerApiRepository: ProviderApiRepository
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var paginateItemsUseCase: PaginateItemsUseCaseImpl

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val logRule = LogRule()

    @Before
    fun setUp() {
        tmdbFilmSearchItemsRepository = mockk()
        providerApiRepository = mockk()
        appDispatchers = mockk {
            every { io } returns testDispatcher
        }

        paginateItemsUseCase = PaginateItemsUseCaseImpl(
            tmdbFilmSearchItemsRepository = tmdbFilmSearchItemsRepository,
            providerApiRepository = providerApiRepository,
            appDispatchers = appDispatchers,
        )
    }

    @Test
    fun `should return success for provider catalog when api call succeeds`() =
        runTest(testDispatcher) {
            val mockProviderApi = mockk<ProviderApi>()
            val providerCatalog = ProviderCatalog(
                providerId = "test-provider",
                name = "Test Catalog",
                canPaginate = true,
                url = "test-url",
            )
            val expectedItems = SearchResponseData(
                page = 1,
                results = listOf(
                    FilmTestDefaults.getFilmSearchItem(
                        title = "Test Movie",
                        filmType = FilmType.MOVIE,
                    ),
                ),
                totalPages = 10,
            )

            every { providerApiRepository.getApi("test-provider") } returns mockProviderApi
            coEvery {
                mockProviderApi.getCatalogItems(
                    page = 1,
                    catalog = providerCatalog,
                )
            } returns expectedItems

            val result = paginateItemsUseCase(providerCatalog, 1)
            advanceUntilIdle()

            expectThat(result).isSuccess()
            expectThat((result as Resource.Success).data).isEqualTo(expectedItems)

            coVerify { mockProviderApi.getCatalogItems(page = 1, catalog = providerCatalog) }
        }

    @Test
    fun `should return failure for provider catalog when api call throws exception`() =
        runTest(testDispatcher) {
            val mockProviderApi = mockk<ProviderApi>()
            val providerCatalog = ProviderCatalog(
                providerId = "test-provider",
                name = "Test Catalog",
                canPaginate = true,
                url = "test-url",
            )
            val exception = RuntimeException("Network error")

            every { providerApiRepository.getApi("test-provider") } returns mockProviderApi
            coEvery {
                mockProviderApi.getCatalogItems(
                    page = 1,
                    catalog = providerCatalog,
                )
            } throws exception

            val result = paginateItemsUseCase(providerCatalog, 1)
            advanceUntilIdle()

            expectThat(result).isFailure()
        }

    @Test
    fun `should return failure for provider catalog when provider api is null`() =
        runTest(testDispatcher) {
            val providerCatalog = ProviderCatalog(
                providerId = "non-existent-provider",
                name = "Test Catalog",
                canPaginate = true,
                url = "test-url",
            )

            every { providerApiRepository.getApi("non-existent-provider") } returns null

            val result = paginateItemsUseCase(providerCatalog, 1)
            advanceUntilIdle()

            expectThat(result).isFailure()
        }

    @Test
    fun `should delegate to tmdb repository for non-provider catalog`() =
        runTest(testDispatcher) {
            val tmdbCatalog = object : Catalog() {
                override val name = "TMDB Catalog"
                override val canPaginate = true
                override val image = null
                override val url = "tmdb-url"
            }

            val expectedResult = Resource.Success(
                SearchResponseData(
                    page = 1,
                    results = listOf(
                        FilmTestDefaults.getFilmSearchItem(
                            title = "TMDB Movie",
                            filmType = FilmType.MOVIE,
                        ),
                    ),
                    totalPages = 5,
                ),
            )

            coEvery {
                tmdbFilmSearchItemsRepository.get(
                    url = "tmdb-url",
                    page = 1,
                )
            } returns expectedResult

            val result = paginateItemsUseCase(tmdbCatalog, 1)

            expectThat(result).isEqualTo(expectedResult)
            coVerify { tmdbFilmSearchItemsRepository.get(url = "tmdb-url", page = 1) }
        }

    @Test
    fun `should handle different page numbers correctly`() =
        runTest(testDispatcher) {
            val tmdbCatalog = object : Catalog() {
                override val name = "TMDB Catalog"
                override val canPaginate = true
                override val url = "tmdb-url"
                override val image = null
            }
            val expectedResult = Resource.Success(
                SearchResponseData(
                    page = 3,
                    results = emptyList<FilmSearchItem>(),
                    totalPages = 5,
                ),
            )

            coEvery {
                tmdbFilmSearchItemsRepository.get(
                    url = "tmdb-url",
                    page = 3,
                )
            } returns expectedResult

            val result = paginateItemsUseCase(tmdbCatalog, 3)

            expectThat(result).isEqualTo(expectedResult)
            coVerify { tmdbFilmSearchItemsRepository.get(url = "tmdb-url", page = 3) }
        }
}
