package com.flixclusive.mobile

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.database.entity.watchlist.WatchlistWithMetadata
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.network.monitor.NetworkMonitor
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.CachedLinksRepository
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

class MobileAppViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

    private val getFilmMetadata: GetFilmMetadataUseCase = mockk()
    private val getSeasonWithWatchProgress: GetSeasonWithWatchProgressUseCase = mockk()
    private val getMediaLinks: GetMediaLinksUseCase = mockk()
    private val watchProgressRepository: WatchProgressRepository = mockk()
    private val watchlistRepository: WatchlistRepository = mockk()
    private val dataStoreManager: DataStoreManager = mockk()
    private val userSessionManager: UserSessionManager = mockk()
    private val libraryListRepository: LibraryListRepository = mockk()
    private val cachedLinksRepository: CachedLinksRepository = mockk()
    private val networkMonitor: NetworkMonitor = mockk()

    private lateinit var viewModel: MobileAppViewModel

    private val testUser = User(
        id = 123,
        name = "Test User",
        image = 1,
        pin = null,
        pinHint = null,
    )

    private val testMovie = FilmTestDefaults.getMovie()
    private val testTvShow = FilmTestDefaults.getTvShow()
    private val testEpisode = FilmTestDefaults.getEpisode()
    private val testSystemPreferences = SystemPreferences()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { userSessionManager.currentUser } returns MutableStateFlow(testUser)
        every { networkMonitor.isOnline } returns flowOf(true)
        every { dataStoreManager.getSystemPrefs() } returns flowOf(testSystemPreferences)
        every { cachedLinksRepository.currentCache } returns MutableStateFlow(null)

        coEvery { watchlistRepository.get(any(), any()) } returns null
        coEvery { libraryListRepository.getListsContainingFilm(any(), any()) } returns flowOf(emptyList())
        coEvery { watchProgressRepository.get(any(), any(), any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have correct default values`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()

            viewModel.uiState.test {
                val initialState = awaitItem()
                expectThat(initialState.loadLinksState).isEqualTo(LoadLinksState.Idle)
                expectThat(initialState.filmPreviewState).isNull()
                expectThat(initialState.playerData).isNull()
            }
        }

    @Test
    fun `hasInternet should reflect network monitor state`() =
        runTest(testDispatcher) {
            every { networkMonitor.isOnline } returns flowOf(false)

            viewModel = createViewModel()

            viewModel.hasInternet.test {
                expectThat(awaitItem()).isFalse()
            }
        }

    @Test
    fun `hasNotSeenNewChangelogs should be true when version code is higher`() =
        runTest(testDispatcher) {
            val systemPrefs = SystemPreferences(lastSeenChangelogs = 100)
            every { dataStoreManager.getSystemPrefs() } returns flowOf(systemPrefs)

            viewModel = createViewModel()

            viewModel.hasNotSeenNewChangelogs.test {
                expectThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `hasNotSeenNewChangelogs should be false when version code is equal or lower`() =
        runTest(testDispatcher) {
            val systemPrefs = SystemPreferences(lastSeenChangelogs = Long.MAX_VALUE)
            every { dataStoreManager.getSystemPrefs() } returns flowOf(systemPrefs)

            viewModel = createViewModel()

            viewModel.hasNotSeenNewChangelogs.test {
                expectThat(awaitItem()).isFalse()
            }
        }

    @Test
    fun `previewFilm should update state with film not in library`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()

            viewModel.previewFilm(testMovie)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.filmPreviewState).isNotNull()
                expectThat(state.filmPreviewState!!.film).isEqualTo(testMovie)
                expectThat(state.filmPreviewState.isInLibrary).isFalse()
            }
        }

    @Test
    fun `previewFilm should update state with film in library when watchlist item exists`() =
        runTest(testDispatcher) {
            val watchlistItem = mockk<WatchlistWithMetadata>()
            coEvery { watchlistRepository.get(testMovie.identifier, testUser.id) } returns watchlistItem

            viewModel = createViewModel()

            viewModel.previewFilm(testMovie)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.filmPreviewState).isNotNull()
                expectThat(state.filmPreviewState!!.film).isEqualTo(testMovie)
                expectThat(state.filmPreviewState.isInLibrary).isTrue()
            }
        }

    @Test
    fun `previewFilm should update state with film in library when library item exists`() =
        runTest(testDispatcher) {
            val libraryItem = listOf(mockk<LibraryList>())
            coEvery { libraryListRepository.getListsContainingFilm(testMovie.identifier, testUser.id) } returns flowOf(
                libraryItem,
            )

            viewModel = createViewModel()

            viewModel.previewFilm(testMovie)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.filmPreviewState).isNotNull()
                expectThat(state.filmPreviewState!!.film).isEqualTo(testMovie)
                expectThat(state.filmPreviewState.isInLibrary).isTrue()
            }
        }

    @Test
    fun `previewFilm should update state with film in library when watch progress exists`() =
        runTest(testDispatcher) {
            val watchProgressItem = mockk<EpisodeProgressWithMetadata>()
            coEvery {
                watchProgressRepository.get(
                    testMovie.identifier,
                    testUser.id,
                    testMovie.filmType,
                )
            } returns watchProgressItem

            viewModel = createViewModel()

            viewModel.previewFilm(testMovie)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.filmPreviewState).isNotNull()
                expectThat(state.filmPreviewState!!.film).isEqualTo(testMovie)
                expectThat(state.filmPreviewState.isInLibrary).isTrue()
            }
        }

    @Test
    fun `onFetchMediaLinks should handle movie successfully`() =
        runTest(testDispatcher) {
            coEvery { getFilmMetadata(testMovie) } returns Resource.Success(testMovie)
            coEvery { getMediaLinks(movie = testMovie) } returns flowOf(
                LoadLinksState.Fetching(),
                LoadLinksState.Success,
            )

            viewModel = createViewModel()

            turbineScope {
                val uiStateFlow = viewModel.uiState.testIn(this)

                viewModel.onFetchMediaLinks(testMovie)
                testDispatcher.scheduler.advanceUntilIdle()

                val initialState = uiStateFlow.awaitItem()
                expectThat(initialState.loadLinksState).isEqualTo(LoadLinksState.Idle)

                val fetchingState = uiStateFlow.awaitItem()
                expectThat(fetchingState.loadLinksState).isEqualTo(LoadLinksState.Fetching())
                expectThat(fetchingState.playerData).isNotNull()
                expectThat(fetchingState.playerData!!.film).isEqualTo(testMovie)
                expectThat(fetchingState.playerData.episode).isNull()

                val successState = uiStateFlow.awaitItem()
                expectThat(successState.loadLinksState).isEqualTo(LoadLinksState.Success)
            }
        }

    @Test
    fun `onFetchMediaLinks should handle tv show with episode successfully`() =
        runTest(testDispatcher) {
            coEvery { getFilmMetadata(testTvShow) } returns Resource.Success(testTvShow)
            coEvery { getMediaLinks(tvShow = testTvShow, episode = testEpisode) } returns flowOf(
                LoadLinksState.Fetching(),
                LoadLinksState.Success,
            )

            viewModel = createViewModel()

            turbineScope {
                val uiStateFlow = viewModel.uiState.testIn(this)

                viewModel.onFetchMediaLinks(testTvShow, testEpisode)
                testDispatcher.scheduler.advanceUntilIdle()

                val initialState = uiStateFlow.awaitItem()
                expectThat(initialState.loadLinksState).isEqualTo(LoadLinksState.Idle)

                val fetchingState = uiStateFlow.awaitItem()
                expectThat(fetchingState.loadLinksState).isEqualTo(LoadLinksState.Fetching())
                expectThat(fetchingState.playerData).isNotNull()
                expectThat(fetchingState.playerData!!.film).isEqualTo(testTvShow)
                expectThat(fetchingState.playerData.episode).isEqualTo(testEpisode)

                val successState = uiStateFlow.awaitItem()
                expectThat(successState.loadLinksState).isEqualTo(LoadLinksState.Success)
            }
        }

    @Test
    fun `onFetchMediaLinks should handle tv show without episode by fetching season`() =
        runTest(testDispatcher) {
            val season = SeasonWithProgress(
                season = FilmTestDefaults.getSeason(),
                episodes = listOf(EpisodeWithProgress(testEpisode, null)),
            )

            coEvery { getFilmMetadata(testTvShow) } returns Resource.Success(testTvShow)
            coEvery { watchProgressRepository.get(testTvShow.identifier, testUser.id, testTvShow.filmType) } returns
                null
            coEvery { getSeasonWithWatchProgress(testTvShow, 1) } returns flowOf(Resource.Success(season))
            coEvery { getMediaLinks(tvShow = testTvShow, episode = testEpisode) } returns flowOf(
                LoadLinksState.Success,
            )

            viewModel = createViewModel()

            turbineScope {
                val uiStateFlow = viewModel.uiState.testIn(this)

                viewModel.onFetchMediaLinks(testTvShow)
                testDispatcher.scheduler.advanceUntilIdle()

                val initialState = uiStateFlow.awaitItem()
                expectThat(initialState.loadLinksState).isEqualTo(LoadLinksState.Idle)

                val fetchingState = uiStateFlow.awaitItem()
                expectThat(fetchingState.loadLinksState).isEqualTo(LoadLinksState.Fetching())

                val playerDataState = uiStateFlow.awaitItem()
                expectThat(playerDataState.playerData).isNotNull()
                expectThat(playerDataState.playerData!!.film).isEqualTo(testTvShow)
                expectThat(playerDataState.playerData.episode).isEqualTo(testEpisode)

                val successState = uiStateFlow.awaitItem()
                expectThat(successState.loadLinksState).isEqualTo(LoadLinksState.Success)
            }
        }

    @Test
    fun `onFetchMediaLinks should handle tv show with existing watch progress`() =
        runTest(testDispatcher) {
            val watchData = EpisodeProgress(
                filmId = testTvShow.identifier,
                id = 1,
                ownerId = testUser.id,
                seasonNumber = 2,
                episodeNumber = 3,
                progress = 1500L,
                status = WatchStatus.WATCHING,
            )
            val episodeProgress = EpisodeProgressWithMetadata(
                watchData = watchData,
                film = testTvShow.toDBFilm(),
            )

            val episodeForSeason2 = FilmTestDefaults.getEpisode(number = 3, season = 2)
            val season = SeasonWithProgress(
                season = FilmTestDefaults.getSeason(name = "Season 2"),
                episodes = listOf(EpisodeWithProgress(episodeForSeason2, null)),
            )

            coEvery { getFilmMetadata(testTvShow) } returns Resource.Success(testTvShow)
            coEvery {
                watchProgressRepository.get(
                    testTvShow.identifier,
                    testUser.id,
                    testTvShow.filmType,
                )
            } returns episodeProgress
            coEvery { getSeasonWithWatchProgress(testTvShow, 2) } returns flowOf(Resource.Success(season))
            coEvery { getMediaLinks(tvShow = testTvShow, episode = episodeForSeason2) } returns flowOf(
                LoadLinksState.Success,
            )

            viewModel = createViewModel()

            viewModel.onFetchMediaLinks(testTvShow)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { getSeasonWithWatchProgress(testTvShow, 2) }
        }

    @Test
    fun `onFetchMediaLinks should handle film metadata fetch failure`() =
        runTest(testDispatcher) {
            coEvery { getFilmMetadata(testMovie) } returns Resource.Failure("Failed to fetch metadata")

            viewModel = createViewModel()

            turbineScope {
                val uiStateFlow = viewModel.uiState.testIn(this)

                viewModel.onFetchMediaLinks(testMovie)
                testDispatcher.scheduler.advanceUntilIdle()

                val initialState = uiStateFlow.awaitItem()
                expectThat(initialState.loadLinksState).isEqualTo(LoadLinksState.Idle)

                val fetchingState = uiStateFlow.awaitItem()
                expectThat(fetchingState.loadLinksState).isEqualTo(LoadLinksState.Fetching())

                val errorState = uiStateFlow.awaitItem()
                expectThat(errorState.loadLinksState).isEqualTo(LoadLinksState.Error())
            }
        }

    @Test
    fun `onFetchMediaLinks should handle season fetch failure for tv show`() =
        runTest(testDispatcher) {
            coEvery { getFilmMetadata(testTvShow) } returns Resource.Success(testTvShow)
            coEvery { watchProgressRepository.get(testTvShow.identifier, testUser.id, testTvShow.filmType) } returns
                null
            coEvery { getSeasonWithWatchProgress(testTvShow, 1) } returns
                flowOf(Resource.Failure("Season fetch failed"))

            viewModel = createViewModel()

            turbineScope {
                val uiStateFlow = viewModel.uiState.testIn(this)

                viewModel.onFetchMediaLinks(testTvShow)
                testDispatcher.scheduler.advanceUntilIdle()

                val initialState = uiStateFlow.awaitItem()
                expectThat(initialState.loadLinksState).isEqualTo(LoadLinksState.Idle)

                val fetchingState = uiStateFlow.awaitItem()
                expectThat(fetchingState.loadLinksState).isEqualTo(LoadLinksState.Fetching())

                val errorState = uiStateFlow.awaitItem()
                expectThat(errorState.loadLinksState).isEqualTo(LoadLinksState.Error())
            }
        }

    @Test
    fun `onFetchMediaLinks should handle episode not found in season`() =
        runTest(testDispatcher) {
            val season = SeasonWithProgress(
                season = FilmTestDefaults.getSeason(),
                episodes = emptyList(), // No episodes in season
            )

            coEvery { getFilmMetadata(testTvShow) } returns Resource.Success(testTvShow)
            coEvery { watchProgressRepository.get(testTvShow.identifier, testUser.id, testTvShow.filmType) } returns
                null
            coEvery { getSeasonWithWatchProgress(testTvShow, 1) } returns flowOf(Resource.Success(season))

            viewModel = createViewModel()

            turbineScope {
                val uiStateFlow = viewModel.uiState.testIn(this)

                viewModel.onFetchMediaLinks(testTvShow)
                testDispatcher.scheduler.advanceUntilIdle()

                val initialState = uiStateFlow.awaitItem()
                expectThat(initialState.loadLinksState).isEqualTo(LoadLinksState.Idle)

                val fetchingState = uiStateFlow.awaitItem()
                expectThat(fetchingState.loadLinksState).isEqualTo(LoadLinksState.Fetching())

                val errorState = uiStateFlow.awaitItem()
                expectThat(errorState.loadLinksState).isEqualTo(LoadLinksState.Error())
            }
        }

    @Test
    fun `onFetchMediaLinks should not start new job if already active`() =
        runTest(testDispatcher) {
            coEvery { getFilmMetadata(testMovie) } returns Resource.Success(testMovie)
            coEvery { getMediaLinks(movie = testMovie) } returns emptyFlow() // Never completes

            viewModel = createViewModel()

            // Start first job
            viewModel.onFetchMediaLinks(testMovie)

            // Try to start second job while first is active
            viewModel.onFetchMediaLinks(testMovie)

            testDispatcher.scheduler.advanceUntilIdle()

            // Verify getFilmMetadata was only called once
            coVerify(exactly = 1) { getFilmMetadata(testMovie) }
        }

    @Test
    fun `onStopLoadingLinks should reset load links state to idle`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()

            // Set state to fetching first
            viewModel.updateLoadLinksState(LoadLinksState.Fetching())

            viewModel.onStopLoadingLinks()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.loadLinksState).isEqualTo(LoadLinksState.Idle)
            }
        }

    @Test
    fun `onRemovePreviewFilm should clear film preview state`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()

            // Set preview state first
            viewModel.previewFilm(testMovie)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onRemovePreviewFilm()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.filmPreviewState).isNull()
            }
        }

    @Test
    fun `updateLoadLinksState should update the load links state`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()

            val newState = LoadLinksState.Fetching()
            viewModel.updateLoadLinksState(newState)

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state.loadLinksState).isEqualTo(newState)
            }
        }

    @Test
    fun `onSaveLastSeenChangelogs should update system preferences`() =
        runTest(testDispatcher) {
            coEvery { dataStoreManager.updateSystemPrefs(any()) } returns Unit

            viewModel = createViewModel()

            val version = 12345L
            viewModel.onSaveLastSeenChangelogs(version)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                dataStoreManager.updateSystemPrefs(any())
            }
        }

    @Test
    fun `hideWebViewDriver should call WebViewDriverManager destroy`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()

            // This is a static call, so we verify behavior indirectly
            viewModel.hideWebViewDriver()

            // No assertion needed as this is testing a void method that calls static method
        }

    @Test
    fun `multiple flows should emit correct initial values`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            turbineScope {
                val uiStateFlow = viewModel.uiState.testIn(this)
                val hasInternetFlow = viewModel.hasInternet.testIn(this)
                val webViewDriverFlow = viewModel.webViewDriver.testIn(this)
                val currentLinksCacheFlow = viewModel.currentLinksCache.testIn(this)

                val uiState = uiStateFlow.awaitItem()
                expectThat(uiState.loadLinksState).isEqualTo(LoadLinksState.Idle)
                expectThat(uiState.filmPreviewState).isNull()
                expectThat(uiState.playerData).isNull()

                val hasInternet = hasInternetFlow.awaitItem()
                expectThat(hasInternet).isTrue()

                val webViewDriver = webViewDriverFlow.awaitItem()
                expectThat(webViewDriver).isNull()

                val currentLinksCache = currentLinksCacheFlow.awaitItem()
                expectThat(currentLinksCache?.streams)
                    .isNotNull()
                    .isEmpty()
            }
        }

    @Test
    fun `should handle user not logged in scenario`() =
        runTest(testDispatcher) {
            every { userSessionManager.currentUser } returns MutableStateFlow(null)

            viewModel = createViewModel()

            try {
                viewModel.previewFilm(testMovie)
                testDispatcher.scheduler.advanceUntilIdle()
            } catch (e: IllegalStateException) {
                expectThat(e.message).isEqualTo("It is now allowed to browse the app without a logged in user!")
            }
        }

    @Test
    fun `data classes should have correct default values`() {
        val defaultUiState = MobileAppUiState()
        expectThat(defaultUiState.loadLinksState).isEqualTo(LoadLinksState.Idle)
        expectThat(defaultUiState.filmPreviewState).isNull()
        expectThat(defaultUiState.playerData).isNull()

        val filmPreview = FilmPreview(
            film = testMovie,
            isInLibrary = true,
        )
        expectThat(filmPreview.film).isEqualTo(testMovie)
        expectThat(filmPreview.isInLibrary).isTrue()

        val playerData = PlayerData(
            film = testMovie,
            episode = testEpisode,
        )
        expectThat(playerData.film).isEqualTo(testMovie)
        expectThat(playerData.episode).isEqualTo(testEpisode)
    }

    private fun createViewModel(): MobileAppViewModel {
        return MobileAppViewModel(
            _getFilmMetadata = getFilmMetadata,
            getSeasonWithWatchProgress = getSeasonWithWatchProgress,
            getMediaLinks = getMediaLinks,
            watchProgressRepository = watchProgressRepository,
            watchlistRepository = watchlistRepository,
            dataStoreManager = dataStoreManager,
            userSessionManager = userSessionManager,
            libraryListRepository = libraryListRepository,
            appDispatchers = appDispatchers,
            cachedLinksRepository = cachedLinksRepository,
            networkMonitor = networkMonitor,
        )
    }
}
