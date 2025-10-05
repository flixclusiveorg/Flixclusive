package com.flixclusive.domain.catalog.usecase.impl

import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.util.collections.CollectionsOperation
import com.flixclusive.data.tmdb.model.TMDBHomeCatalog
import com.flixclusive.data.tmdb.model.TMDBHomeCatalogs
import com.flixclusive.data.tmdb.repository.TMDBHomeCatalogRepository
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.provider.ProviderApi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

class GetHomeCatalogsUseCaseImplTest {
    private lateinit var watchProgressRepository: WatchProgressRepository
    private lateinit var tmdbHomeCatalogRepository: TMDBHomeCatalogRepository
    private lateinit var userSessionManager: UserSessionManager
    private lateinit var providerApiRepository: ProviderApiRepository
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var getHomeCatalogsUseCase: GetHomeCatalogsUseCaseImpl

    private val testDispatcher = StandardTestDispatcher()
    private val testUser = DatabaseTestDefaults.getUser()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        watchProgressRepository = mockk()
        tmdbHomeCatalogRepository = mockk()
        userSessionManager = mockk()
        providerApiRepository = mockk()
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        getHomeCatalogsUseCase = GetHomeCatalogsUseCaseImpl(
            watchProgressRepository = watchProgressRepository,
            tmdbHomeCatalogRepository = tmdbHomeCatalogRepository,
            userSessionManager = userSessionManager,
            providerApiRepository = providerApiRepository,
            appDispatchers = appDispatchers,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should return catalogs when user is logged in`() =
        runTest(testDispatcher) {
            val requiredCatalog = TMDBHomeCatalog(
                name = "Trending",
                required = true,
                url = "trending/all/day",
            )
            val optionalCatalog = TMDBHomeCatalog(
                name = "Popular Movies",
                required = false,
                url = "movie/popular",
            )
            val testFilm = FilmTestDefaults
                .getMovie(
                    title = "Test Movie",
                    tmdbId = 123,
                    recommendations = List(10) {
                        FilmTestDefaults.getFilmSearchItem(
                            title = "Recommended Movie $it",
                            tmdbId = 123 + it,
                        )
                    },
                ).toDBFilm()
            val watchProgress = DatabaseTestDefaults.getMovieProgress(
                ownerId = testUser.id,
                filmId = testFilm.id,
            )
            val tmdbCatalogs = mockk<TMDBHomeCatalogs> {
                every { all } returns listOf(requiredCatalog)
                every { tv } returns emptyList()
                every { movie } returns listOf(optionalCatalog)
            }

            every { userSessionManager.currentUser } returns MutableStateFlow(testUser).asStateFlow()
            coEvery {
                watchProgressRepository.getRandoms(
                    ownerId = 1,
                    count = any(),
                )
            } returns flowOf(
                listOf(
                    MovieProgressWithMetadata(
                        watchData = watchProgress,
                        film = testFilm,
                    ),
                ),
            )
            coEvery { tmdbHomeCatalogRepository.getAllCatalogs() } returns tmdbCatalogs

            getHomeCatalogsUseCase().test {
                val catalogs = awaitItem()

                expectThat(catalogs).hasSize(3) // required + recommendation + optional
                expectThat(catalogs).contains(requiredCatalog)
                expectThat(catalogs).contains(optionalCatalog)

                val recommendationCatalog = catalogs.find { it.name.contains("If you liked") }
                expectThat(recommendationCatalog).isEqualTo(
                    TMDBHomeCatalog(
                        name = "If you liked Test Movie",
                        required = false,
                        url = "movie/${watchProgress.filmId}/recommendations?language=en-US",
                    ),
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `should include provider catalogs when available`() =
        runTest(testDispatcher) {
            val tmdbCatalog = TMDBHomeCatalog(
                name = "Trending",
                required = true,
                url = "trending/all/day",
            )

            val tmdbCatalogs = mockk<TMDBHomeCatalogs> {
                every { all } returns listOf(tmdbCatalog)
                every { tv } returns emptyList()
                every { movie } returns emptyList()
            }

            val providerCatalog = ProviderCatalog(
                providerId = "test-provider",
                name = "Provider Catalog",
                canPaginate = true,
                url = "provider-url",
            )

            val providerApi = mockk<ProviderApi> {
                every { catalogs } returns listOf(providerCatalog)
            }

            val modifiedProviderApiRepository = mockk<ProviderApiRepository> {
                every { observe() } returns MutableStateFlow(
                    CollectionsOperation.Map.Add(
                        key = "test-provider",
                        value = providerApi,
                    ),
                ).asSharedFlow()
            }

            every { userSessionManager.currentUser } returns MutableStateFlow(testUser).asStateFlow()
            coEvery {
                watchProgressRepository.getRandoms(
                    ownerId = 1,
                    count = any(),
                )
            } returns flowOf(emptyList())
            coEvery { tmdbHomeCatalogRepository.getAllCatalogs() } returns tmdbCatalogs

            val modifiedGetHomeCatalogsUseCase = GetHomeCatalogsUseCaseImpl(
                watchProgressRepository = watchProgressRepository,
                tmdbHomeCatalogRepository = tmdbHomeCatalogRepository,
                userSessionManager = userSessionManager,
                providerApiRepository = modifiedProviderApiRepository,
                appDispatchers = appDispatchers,
            )

            modifiedGetHomeCatalogsUseCase().test {
                skipItems(1)
                val catalogs = awaitItem()

                expectThat(catalogs).hasSize(2)
                expectThat(catalogs).contains(tmdbCatalog)
                expectThat(catalogs).contains(providerCatalog)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `should filter out watch history items without enough recommendations`() =
        runTest(testDispatcher) {
            val requiredCatalog = TMDBHomeCatalog(
                name = "Trending",
                required = true,
                url = "trending/all/day",
            )
            val testFilm = FilmTestDefaults
                .getMovie(
                    title = "Test Movie",
                    tmdbId = 123,
                    recommendations = List(2) {
                        FilmTestDefaults.getFilmSearchItem(
                            title = "Recommended Movie $it",
                            tmdbId = 123 + it,
                        )
                    },
                ).toDBFilm()
            val watchProgress = DatabaseTestDefaults.getMovieProgress(filmId = testFilm.id)
            val tmdbCatalogs = mockk<TMDBHomeCatalogs> {
                every { all } returns listOf(requiredCatalog)
                every { tv } returns emptyList()
                every { movie } returns emptyList()
            }

            every { userSessionManager.currentUser } returns MutableStateFlow(testUser).asStateFlow()
            coEvery {
                watchProgressRepository.getRandoms(
                    ownerId = 1,
                    count = any(),
                )
            } returns flowOf(
                listOf(
                    MovieProgressWithMetadata(
                        watchData = watchProgress,
                        film = testFilm,
                    ),
                ),
            )
            coEvery { tmdbHomeCatalogRepository.getAllCatalogs() } returns tmdbCatalogs

            getHomeCatalogsUseCase().test {
                val catalogs = awaitItem()

                expectThat(catalogs).hasSize(1)
                expectThat(catalogs).contains(requiredCatalog)

                val recommendationCatalog = catalogs.find { it.name.contains("If you liked") }
                expectThat(recommendationCatalog).isEqualTo(null)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `should filter out non-tmdb films from recommendations`() =
        runTest(testDispatcher) {
            val requiredCatalog = TMDBHomeCatalog(
                name = "Trending",
                required = true,
                url = "trending/all/day",
            )
            val testFilm = FilmTestDefaults
                .getMovie(
                    title = "Non-TMDB Movie",
                    providerId = "test-provider",
                ).toDBFilm()
            val watchProgress = DatabaseTestDefaults.getMovieProgress(
                filmId = testFilm.id,
            )
            val tmdbCatalogs = mockk<TMDBHomeCatalogs> {
                every { all } returns listOf(requiredCatalog)
                every { tv } returns emptyList()
                every { movie } returns emptyList()
            }

            every { userSessionManager.currentUser } returns MutableStateFlow(testUser).asStateFlow()
            coEvery {
                watchProgressRepository.getRandoms(
                    ownerId = 1,
                    count = any(),
                )
            } returns flowOf(
                listOf(
                    MovieProgressWithMetadata(
                        watchData = watchProgress,
                        film = testFilm,
                    ),
                ),
            )
            coEvery { tmdbHomeCatalogRepository.getAllCatalogs() } returns tmdbCatalogs

            getHomeCatalogsUseCase().test {
                val catalogs = awaitItem()

                expectThat(catalogs).hasSize(1)
                expectThat(catalogs).contains(requiredCatalog)

                val recommendationCatalog = catalogs.find { it.name.contains("If you liked") }
                expectThat(recommendationCatalog).isEqualTo(null)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `should prioritize trending catalog at the top`() =
        runTest(testDispatcher) {
            val trendingCatalog = TMDBHomeCatalog(
                name = "Trending",
                required = true,
                url = "trending/all/day",
            )
            val otherCatalog = TMDBHomeCatalog(
                name = "Popular Movies",
                required = false,
                url = "movie/popular",
            )
            val tmdbCatalogs = mockk<TMDBHomeCatalogs> {
                every { all } returns listOf(trendingCatalog)
                every { tv } returns emptyList()
                every { movie } returns listOf(otherCatalog)
            }

            every { userSessionManager.currentUser } returns MutableStateFlow(testUser).asStateFlow()
            coEvery {
                watchProgressRepository.getRandoms(
                    ownerId = 1,
                    count = any(),
                )
            } returns flowOf(emptyList())
            coEvery { tmdbHomeCatalogRepository.getAllCatalogs() } returns tmdbCatalogs

            getHomeCatalogsUseCase().test {
                val catalogs = awaitItem()

                expectThat(catalogs).hasSize(2)
                expectThat(catalogs.first()).isEqualTo(trendingCatalog)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `should handle empty watch history gracefully`() =
        runTest(testDispatcher) {
            val requiredCatalog = TMDBHomeCatalog(
                name = "Trending",
                required = true,
                url = "trending/all/day",
            )
            val tmdbCatalogs = mockk<TMDBHomeCatalogs> {
                every { all } returns listOf(requiredCatalog)
                every { tv } returns emptyList()
                every { movie } returns emptyList()
            }

            every { userSessionManager.currentUser } returns MutableStateFlow(testUser).asStateFlow()
            coEvery {
                watchProgressRepository.getRandoms(
                    ownerId = 1,
                    count = any(),
                )
            } returns flowOf(emptyList())
            coEvery { tmdbHomeCatalogRepository.getAllCatalogs() } returns tmdbCatalogs

            getHomeCatalogsUseCase().test {
                val catalogs = awaitItem()

                expectThat(catalogs).hasSize(1)
                expectThat(catalogs).contains(requiredCatalog)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
