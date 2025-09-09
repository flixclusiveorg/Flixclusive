package com.flixclusive.feature.mobile.home

import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.pagination.PagingDataState
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.domain.catalog.usecase.GetHomeCatalogsUseCase
import com.flixclusive.domain.catalog.usecase.GetHomeHeaderUseCase
import com.flixclusive.domain.catalog.usecase.PaginateItemsUseCase
import com.flixclusive.domain.provider.usecase.get.GetEpisodeUseCase
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.Catalog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue
import com.flixclusive.core.strings.R as LocaleR

class HomeScreenViewModelTest {
    private lateinit var viewModel: HomeScreenViewModel
    private lateinit var appDispatchers: AppDispatchers

    // Mock dependencies
    private val getHomeCatalogs = mockk<GetHomeCatalogsUseCase>()
    private val userSessionManager = mockk<UserSessionManager>()
    private val dataStoreManager = mockk<DataStoreManager>()
    private val getHomeHeader = mockk<GetHomeHeaderUseCase>()
    private val paginateItems = mockk<PaginateItemsUseCase>()
    private val getEpisode = mockk<GetEpisodeUseCase>()
    private val getFilmMetadata = mockk<GetFilmMetadataUseCase>()
    private val watchProgressRepository = mockk<WatchProgressRepository>()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testUser = User(id = 1, name = "Test User", image = 0)

    private val testMovie = Movie(
        id = "1",
        title = "Test Movie",
        posterImage = null,
        homePage = null,
        providerId = DEFAULT_FILM_SOURCE_NAME,
    )

    private val testTvShow = TvShow(
        id = "2",
        title = "Test TV Show",
        providerId = DEFAULT_FILM_SOURCE_NAME,
        homePage = null,
        posterImage = null,
        adult = false,
        backdropImage = null,
        imdbId = null,
        tmdbId = null,
        releaseDate = null,
        rating = null,
        language = null,
        overview = null,
        year = null,
        logoImage = null,
        genres = emptyList(),
        customProperties = emptyMap(),
        seasons = emptyList(),
    )

    private val testCatalog = object : Catalog() {
        override val name: String = "Popular Movies"
        override val url: String = "popular-movies"
        override val image: String? = null
        override val canPaginate: Boolean = true
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        appDispatchers = object : AppDispatchers {
            override val default = testDispatcher
            override val io = testDispatcher
            override val main = testDispatcher
            override val unconfined = testDispatcher
            override val ioScope get() = TestScope(testDispatcher)
            override val defaultScope get() = TestScope(testDispatcher)
            override val mainScope get() = TestScope(testDispatcher)
        }

        // Setup default mock behaviors
        every { userSessionManager.currentUser } returns MutableStateFlow(testUser)
        every { getHomeCatalogs() } returns flowOf(listOf(testCatalog))
        every { dataStoreManager.getUserPrefs(any(), any<kotlin.reflect.KClass<UiPreferences>>()) } returns
            flowOf(UiPreferences(shouldShowTitleOnCards = false))
        every { watchProgressRepository.getAllAsFlow(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeScreenViewModel {
        return HomeScreenViewModel(
            getHomeCatalogs = getHomeCatalogs,
            userSessionManager = userSessionManager,
            appDispatchers = appDispatchers,
            dataStoreManager = dataStoreManager,
            getHomeHeader = getHomeHeader,
            paginateItems = paginateItems,
            getEpisode = getEpisode,
            getFilmMetadata = getFilmMetadata,
            watchProgressRepository = watchProgressRepository,
        )
    }

    @Test
    fun `when created then loads home header`() =
        runTest(testDispatcher) {
            coEvery { getHomeHeader() } returns Resource.Success(testMovie)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.itemHeader).isEqualTo(testMovie)
                expectThat(state.itemHeaderError).isNull()
            }

            coVerify { getHomeHeader() }
        }

    @Test
    fun `when home header load fails then sets error`() =
        runTest(testDispatcher) {
            val errorMessage = UiText.StringValue("Network error")
            coEvery { getHomeHeader() } returns Resource.Failure(error = errorMessage)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.itemHeader).isNull()
                expectThat(state.itemHeaderError).isEqualTo(errorMessage)
            }
        }

    @Test
    fun `when loadHomeHeader called then updates state`() =
        runTest(testDispatcher) {
            coEvery { getHomeHeader() } returns Resource.Success(testMovie)
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.loadHomeHeader()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.itemHeader).isEqualTo(testMovie)
                expectThat(state.itemHeaderError).isNull()
            }
        }

    @Test
    fun `when catalogs emitted then initializes pagination states`() =
        runTest(testDispatcher) {
            every { getHomeCatalogs() } returns flowOf(listOf(testCatalog))
            coEvery { getHomeHeader() } returns Resource.Success(testMovie)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.items).hasSize(1)
                expectThat(state.pagingStates).hasSize(1)
                expectThat(state.pagingStates[testCatalog.url]?.hasNext).isTrue()
                expectThat(state.pagingStates[testCatalog.url]?.page).isEqualTo(1)
            }
        }

    @Test
    fun `when non-paginating catalog emitted then sets correct pagination state`() =
        runTest(testDispatcher) {
            val nonPaginatingCatalog = object : Catalog() {
                override val name: String = "Featured Movies"
                override val url: String = "featured-movies"
                override val image: String? = null
                override val canPaginate: Boolean = false
            }
            every { getHomeCatalogs() } returns flowOf(listOf(nonPaginatingCatalog))
            coEvery { getHomeHeader() } returns Resource.Success(testMovie)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                val pagingState = state.pagingStates[nonPaginatingCatalog.url]
                expectThat(pagingState?.hasNext).isFalse()
                expectThat(pagingState?.state).isA<PagingDataState.Error>()
            }
        }

    @Test
    fun `when paginate called with success then updates items and state`() =
        runTest(testDispatcher) {
            val searchItems = listOf(
                FilmSearchItem(
                    id = "1",
                    title = "Test Movie 1",
                    filmType = FilmType.MOVIE,
                    providerId = DEFAULT_FILM_SOURCE_NAME,
                    homePage = null,
                    posterImage = null,
                ),
                FilmSearchItem(
                    id = "2",
                    title = "Test Movie 2",
                    filmType = FilmType.MOVIE,
                    providerId = DEFAULT_FILM_SOURCE_NAME,
                    homePage = null,
                    posterImage = null,
                ),
            )
            val searchResponse = SearchResponseData(
                page = 1,
                results = searchItems,
                totalPages = 5,
                hasNextPage = true,
            )

            coEvery { getHomeHeader() } returns Resource.Success(testMovie)
            coEvery { paginateItems(testCatalog, 1) } returns Resource.Success(searchResponse)

            viewModel = createViewModel()
            viewModel.paginate(testCatalog, 1)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.items[testCatalog.url]?.size).isEqualTo(2)
                expectThat(state.pagingStates[testCatalog.url]?.page).isEqualTo(1)
                expectThat(state.pagingStates[testCatalog.url]?.hasNext).isTrue()
            }
        }

    @Test
    fun `when paginate fails then sets error state`() =
        runTest(testDispatcher) {
            val errorMessage = UiText.StringValue("Pagination failed")
            coEvery { getHomeHeader() } returns Resource.Success(testMovie)
            coEvery { paginateItems(testCatalog, 1) } returns Resource.Failure(error = errorMessage)

            viewModel = createViewModel()
            viewModel.paginate(testCatalog, 1)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                val pagingState = state.pagingStates[testCatalog.url]
                expectThat(pagingState?.state).isA<PagingDataState.Error>().and {
                   get { error }.isEqualTo(errorMessage)
                }
            }
        }

    @Test
    fun `when paginate reaches max pages then sets hasNext to false`() =
        runTest(testDispatcher) {
            val searchResponse = SearchResponseData(
                page = HomeUiState.MAX_PAGINATION_PAGES,
                results = listOf(
                    FilmSearchItem(
                        id = "1",
                        title = "Test",
                        filmType = FilmType.MOVIE,
                        providerId = DEFAULT_FILM_SOURCE_NAME,
                        homePage = null,
                        posterImage = null,
                    ),
                ),
                totalPages = 10,
                hasNextPage = true,
            )

            coEvery { getHomeHeader() } returns Resource.Success(testMovie)
            coEvery { paginateItems(testCatalog, HomeUiState.MAX_PAGINATION_PAGES) } returns
                Resource.Success(searchResponse)

            viewModel = createViewModel()
            viewModel.paginate(testCatalog, HomeUiState.MAX_PAGINATION_PAGES)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                val pagingState = state.pagingStates[testCatalog.url]
                expectThat(pagingState?.hasNext).isFalse()
                expectThat(pagingState?.state).isA<PagingDataState.Error>().and {
                    get { error }.isA<UiText.StringResource>().and {
                        get { stringId }.isEqualTo(LocaleR.string.end_of_list)
                    }
                }
            }
        }

    @Test
    fun `when showFilmTitles observed then returns correct value`() =
        runTest(testDispatcher) {
            every { dataStoreManager.getUserPrefs(any(), any<kotlin.reflect.KClass<UiPreferences>>()) } returns
                flowOf(UiPreferences(shouldShowTitleOnCards = true))

            coEvery { getHomeHeader() } returns Resource.Failure("")

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.showFilmTitles.test {
                expectThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `when continue watching contains unfinished movie then includes in list`() =
        runTest(testDispatcher) {
            val dbFilm = DBFilm(
                id = "1",
                title = "Test Movie",
                filmType = FilmType.MOVIE,
            )
            val movieProgress = MovieProgress(
                ownerId = testUser.id,
                filmId = dbFilm.id,
                progress = 5000L,
                status = WatchStatus.WATCHING,
            )
            val watchProgressWithMetadata = MovieProgressWithMetadata(
                watchData = movieProgress,
                film = dbFilm,
            )

            every { watchProgressRepository.getAllAsFlow(testUser.id) } returns
                flowOf(listOf(watchProgressWithMetadata))
            coEvery { getHomeHeader() } returns Resource.Success(testMovie)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.continueWatchingItems.test {
                val items = awaitItem()
                expectThat(items).hasSize(1)
                expectThat(items.first().film).isEqualTo(dbFilm)
            }
        }

    @Test
    fun `when continue watching contains finished movie then excludes from list`() =
        runTest(testDispatcher) {
            val dbFilm = DBFilm(
                id = "1",
                title = "Test Movie",
                filmType = FilmType.MOVIE,
            )
            val movieProgress = MovieProgress(
                ownerId = testUser.id,
                filmId = dbFilm.id,
                progress = 7200000L,
                status = WatchStatus.COMPLETED,
            )
            val watchProgressWithMetadata = MovieProgressWithMetadata(
                watchData = movieProgress,
                film = dbFilm,
            )

            every { watchProgressRepository.getAllAsFlow(testUser.id) } returns
                flowOf(listOf(watchProgressWithMetadata))
            coEvery { getHomeHeader() } returns Resource.Success(testMovie)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.continueWatchingItems.test {
                val items = awaitItem()
                expectThat(items).hasSize(0)
            }
        }

    @Test
    fun `when continue watching contains unfinished episode then includes in list`() =
        runTest(testDispatcher) {
            val dbFilm = DBFilm(
                id = "1",
                title = "Test TV Show",
                filmType = FilmType.TV_SHOW,
            )
            val episodeProgress = EpisodeProgress(
                ownerId = testUser.id,
                filmId = dbFilm.id,
                seasonNumber = 1,
                episodeNumber = 1,
                progress = 1000L,
                status = WatchStatus.WATCHING,
            )
            val watchProgressWithMetadata = EpisodeProgressWithMetadata(
                watchData = episodeProgress,
                film = dbFilm,
            )

            every { watchProgressRepository.getAllAsFlow(testUser.id) } returns
                flowOf(listOf(watchProgressWithMetadata))
            coEvery { getHomeHeader() } returns Resource.Success(testMovie)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.continueWatchingItems.test {
                val items = awaitItem()
                expectThat(items).hasSize(1)
                expectThat(items.first().film).isEqualTo(dbFilm)
            }
        }

    @Test
    fun `when continue watching contains finished episode with next episode then creates next episode progress`() =
        runTest(testDispatcher) {
            val dbFilm = DBFilm(
                id = "1",
                title = "Test TV Show",
                filmType = FilmType.TV_SHOW,
            )
            val episodeProgress = EpisodeProgress(
                ownerId = testUser.id,
                filmId = dbFilm.id,
                seasonNumber = 1,
                episodeNumber = 1,
                progress = 2700000L,
                status = WatchStatus.COMPLETED,
            )
            val watchProgressWithMetadata = EpisodeProgressWithMetadata(
                watchData = episodeProgress,
                film = dbFilm,
            )
            val nextEpisode = Episode(
                id = "episode-2",
                number = 2,
                season = 1,
                title = "Episode 2",
            )
            val nextEpisodeProgress = EpisodeProgress(
                ownerId = testUser.id,
                filmId = dbFilm.id,
                seasonNumber = 1,
                episodeNumber = 2,
                progress = 0L,
                status = WatchStatus.WATCHING,
            )
            val nextWatchProgressWithMetadata = EpisodeProgressWithMetadata(
                watchData = nextEpisodeProgress,
                film = dbFilm,
            )

            every { watchProgressRepository.getAllAsFlow(testUser.id) } returns
                flowOf(listOf(watchProgressWithMetadata))
            coEvery { getHomeHeader() } returns Resource.Success(testMovie)
            coEvery { getFilmMetadata(dbFilm) } returns Resource.Success(testTvShow)
            coEvery { getEpisode(testTvShow, 1, 2) } returns nextEpisode
            coEvery { watchProgressRepository.insert(any<EpisodeProgress>(), dbFilm) } returns 2L
            coEvery {
                watchProgressRepository.get(2L, FilmType.TV_SHOW)
            } returns nextWatchProgressWithMetadata

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.continueWatchingItems.test {
                val items = awaitItem()
                expectThat(items).hasSize(1)
                expectThat(items.first().watchData).isEqualTo(nextEpisodeProgress)
            }

            coVerify { watchProgressRepository.insert(any<EpisodeProgress>(), dbFilm) }
        }

    @Test
    fun `when continue watching contains finished episode with no next episode then excludes from list`() =
        runTest(testDispatcher) {
            val dbFilm = DBFilm(
                id = "1",
                title = "Test TV Show",
                filmType = FilmType.TV_SHOW,
            )
            val episodeProgress = EpisodeProgress(
                ownerId = testUser.id,
                filmId = dbFilm.id,
                seasonNumber = 1,
                episodeNumber = 10,
                progress = 2700000L,
                status = WatchStatus.COMPLETED,
            )
            val watchProgressWithMetadata = EpisodeProgressWithMetadata(
                watchData = episodeProgress,
                film = dbFilm,
            )

            every { watchProgressRepository.getAllAsFlow(testUser.id) } returns
                flowOf(listOf(watchProgressWithMetadata))
            coEvery { getHomeHeader() } returns Resource.Success(testMovie)
            coEvery { getFilmMetadata(dbFilm) } returns Resource.Success(testTvShow)
            coEvery { getEpisode(testTvShow, 1, 11) } returns null

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.continueWatchingItems.test {
                val items = awaitItem()
                expectThat(items).hasSize(0)
            }
        }
}
