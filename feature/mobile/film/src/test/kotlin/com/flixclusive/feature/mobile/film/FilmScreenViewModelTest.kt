package com.flixclusive.feature.mobile.film

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.library.UserWithLibraryListsAndItems
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.UiPreferences
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.core.testing.provider.ProviderTestDefaults
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.database.usecase.ToggleWatchProgressStatusUseCase
import com.flixclusive.domain.database.usecase.ToggleWatchlistStatusUseCase
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.domain.provider.usecase.get.GetFilmMetadataUseCase
import com.flixclusive.domain.provider.usecase.get.GetSeasonWithWatchProgressUseCase
import com.flixclusive.feature.mobile.library.common.util.LibraryListUtil
import com.flixclusive.model.film.Film
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FilmScreenViewModelTest {
    private val context: Context = mockk(relaxed = true)
    private val dataStoreManager: DataStoreManager = mockk(relaxed = true)
    private val getSeasonWithWatchProgress: GetSeasonWithWatchProgressUseCase = mockk(relaxed = true)
    private val userSessionManager: UserSessionManager = mockk(relaxed = true)
    private val getFilmMetadata: GetFilmMetadataUseCase = mockk(relaxed = true)
    private val libraryListRepository: LibraryListRepository = mockk(relaxed = true)
    private val providerRepository: ProviderRepository = mockk(relaxed = true)
    private val toggleWatchProgressStatus: ToggleWatchProgressStatusUseCase = mockk(relaxed = true)
    private val toggleWatchlistStatus: ToggleWatchlistStatusUseCase = mockk(relaxed = true)
    private val watchProgressRepository: WatchProgressRepository = mockk(relaxed = true)
    private val watchlistRepository: WatchlistRepository = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val appDispatchers = object : AppDispatchers {
        override val default: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val main: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
        override val ioScope: CoroutineScope = testScope
        override val defaultScope: CoroutineScope = testScope
        override val mainScope: CoroutineScope = testScope
    }

    private val testUser = DatabaseTestDefaults.getUser()
    private val testPartialMovie = FilmTestDefaults.getFilmSearchItem()
    private val testMovie = FilmTestDefaults.getMovie()
    private val testTvShow = FilmTestDefaults.getTvShow()
    private val testProviderMetadata = ProviderTestDefaults.getProviderMetadata()

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: FilmScreenViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { userSessionManager.currentUser } returns MutableStateFlow(testUser)
        every { dataStoreManager.getUserPrefs(any<Preferences.Key<String>>(), UiPreferences::class) } returns
            flowOf(UiPreferences())
        every { watchProgressRepository.getAsFlow(any(), any(), any()) } returns flowOf(null)
        every { libraryListRepository.getUserWithListsAndItems(any()) } returns
            flowOf(UserWithLibraryListsAndItems(testUser, emptyList()))
        every { watchProgressRepository.getAllAsFlow(any()) } returns flowOf(emptyList())
        every { watchlistRepository.getAllAsFlow(any()) } returns flowOf(emptyList())
        every { providerRepository.getProviderMetadata(any()) } returns testProviderMetadata
        coEvery { getFilmMetadata(any()) } returns Resource.Success(testMovie)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(film: Film = testPartialMovie) {
        savedStateHandle = mockk(relaxed = true) {
            every { get<Film?>("film") } returns film
        }

        viewModel = FilmScreenViewModel(
            context = context,
            dataStoreManager = dataStoreManager,
            getSeasonWithWatchProgress = getSeasonWithWatchProgress,
            savedStateHandle = savedStateHandle,
            userSessionManager = userSessionManager,
            appDispatchers = appDispatchers,
            getFilmMetadata = getFilmMetadata,
            libraryListRepository = libraryListRepository,
            providerRepository = providerRepository,
            toggleWatchProgressStatus = toggleWatchProgressStatus,
            toggleWatchlistStatus = toggleWatchlistStatus,
            watchProgressRepository = watchProgressRepository,
            watchlistRepository = watchlistRepository,
        )
    }

    @Test
    fun `initialization with movie should load metadata successfully`() =
        runTest(testDispatcher) {
            createViewModel(testMovie)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state) {
                    get { isLoading }.isFalse()
                    get { error }.isNull()
                    get { provider }.isEqualTo(testProviderMetadata)
                    get { screenState }.isEqualTo(FilmScreenState.Success)
                }
            }

            viewModel.metadata.test {
                expectThat(awaitItem()).isEqualTo(testMovie)
            }
        }

    @Test
    fun `initialization with tv show should set initial selected season`() =
        runTest(testDispatcher) {
            createViewModel(testTvShow)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state) {
                    get { selectedSeason }.isEqualTo(10) // Should select latest season (10 from FilmTestDefaults)
                    get { screenState }.isEqualTo(FilmScreenState.Success)
                }
            }
        }

    @Test
    fun `initialization with partial film data should fetch metadata`() =
        runTest(testDispatcher) {
            val partialMovie = FilmTestDefaults.getFilmSearchItem(
                tmdbId = null,
                imdbId = null,
                title = "Partial Movie",
                homePage = null,
                releaseDate = null,
                backdropImage = null,
                posterImage = null,
            )

            createViewModel(partialMovie)
            advanceUntilIdle()

            coVerify { getFilmMetadata(partialMovie) }

            viewModel.metadata.test {
                expectThat(awaitItem()).isEqualTo(testMovie)
            }
        }

    @Test
    fun `initialization should handle metadata fetch failure`() =
        runTest(testDispatcher) {
            val errorMessage = UiText.from("Network error")
            coEvery { getFilmMetadata(any()) } returns Resource.Failure(errorMessage)

            createViewModel()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state) {
                    get { isLoading }.isFalse()
                    get { error }.isEqualTo(errorMessage)
                    get { screenState }.isEqualTo(FilmScreenState.Error)
                }
            }
        }

    @Test
    fun `onRetry should refetch metadata when not loading`() =
        runTest(testDispatcher) {
            val errorMessage = UiText.from("Network error")
            coEvery { getFilmMetadata(any()) } returns Resource.Failure(errorMessage)

            createViewModel()
            advanceUntilIdle()

            coEvery { getFilmMetadata(any()) } returns Resource.Success(testMovie)

            viewModel.onRetry()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                expectThat(state) {
                    get { error }.isNull()
                    get { screenState }.isEqualTo(FilmScreenState.Success)
                }
            }
        }

    @Test
    fun `onSeasonChange should update selected season`() =
        runTest(testDispatcher) {
            createViewModel(testTvShow)
            advanceUntilIdle()

            viewModel.onSeasonChange(1)

            viewModel.uiState.test {
                expectThat(awaitItem()).get { selectedSeason }.isEqualTo(1)
            }
        }

    @Test
    fun `onLibrarySheetQueryChange should update query`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onLibrarySheetQueryChange("test query")

            viewModel.librarySheetQuery.test {
                expectThat(awaitItem()).isEqualTo("test query")
            }
        }

    @Test
    fun `toggleOnLibrary with watchlist id should call toggle watchlist`() =
        runTest(testDispatcher) {
            coEvery { toggleWatchlistStatus(any()) } just runs

            createViewModel()
            advanceUntilIdle()

            viewModel.toggleOnLibrary(LibraryListUtil.WATCHLIST_LIB_ID)
            advanceUntilIdle()

            coVerify { toggleWatchlistStatus(testMovie) }
        }

    @Test
    fun `toggleOnLibrary with watch progress id should call toggle watch progress`() =
        runTest(testDispatcher) {
            coEvery { toggleWatchProgressStatus(any()) } just runs

            createViewModel()
            advanceUntilIdle()

            viewModel.toggleOnLibrary(LibraryListUtil.WATCH_PROGRESS_LIB_ID)
            advanceUntilIdle()

            coVerify { toggleWatchProgressStatus(testMovie) }
        }

    @Test
    fun `toggleOnLibrary with custom list should add item when not present`() =
        runTest(testDispatcher) {
            val customListId = 123
            coEvery { libraryListRepository.insertItem(any(), any()) } returns 1L

            createViewModel()
            advanceUntilIdle()

            viewModel.toggleOnLibrary(customListId)
            advanceUntilIdle()

            coVerify {
                libraryListRepository.insertItem(
                    item = match<LibraryListItem> {
                        it.listId == customListId &&
                            it.filmId == testPartialMovie.identifier
                    },
                    film = testMovie
                )
            }
        }

    @Test
    fun `toggleEpisodeOnLibrary should add episode progress when not present`() =
        runTest(testDispatcher) {
            val episode = FilmTestDefaults.getEpisode()
            val episodeWithProgress = EpisodeWithProgress(episode, null)

            coEvery { watchProgressRepository.insert(any(), any()) } returns 1L

            createViewModel(testTvShow)
            advanceUntilIdle()

            viewModel.toggleEpisodeOnLibrary(episodeWithProgress)
            advanceUntilIdle()

            coVerify {
                watchProgressRepository.insert(
                    film = testTvShow,
                    item = match<EpisodeProgress> {
                        it.ownerId == testUser.id &&
                            it.filmId == testTvShow.id &&
                            it.seasonNumber == 1 &&
                            it.episodeNumber == 1 &&
                            it.status == WatchStatus.COMPLETED
                    },
                )
            }
        }

    @Test
    fun `toggleEpisodeOnLibrary should remove episode progress when present`() =
        runTest(testDispatcher) {
            val episode = FilmTestDefaults.getEpisode()
            val progress = DatabaseTestDefaults.getEpisodeProgress(
                filmId = testTvShow.identifier,
                ownerId = testUser.id,
            )
            val episodeWithProgress = EpisodeWithProgress(episode, progress)

            coEvery { watchProgressRepository.delete(any(), any()) } just runs

            createViewModel(testTvShow)
            advanceUntilIdle()

            viewModel.toggleEpisodeOnLibrary(episodeWithProgress)
            advanceUntilIdle()

            coVerify { watchProgressRepository.delete(progress.id, testTvShow.filmType) }
        }

    @Test
    fun `createLibrary should create new list and add film to it`() =
        runTest(testDispatcher) {
            val listName = "My List"
            val listDescription = "Test Description"
            val newListId = 456

            coEvery { libraryListRepository.insertList(any()) } returns newListId
            coEvery { libraryListRepository.insertItem(any(), any()) } returns 1L

            createViewModel()
            advanceUntilIdle()

            viewModel.createLibrary(listName, listDescription)
            advanceUntilIdle()

            coVerify {
                libraryListRepository.insertList(
                    match<LibraryList> {
                        it.name == listName &&
                            it.description == listDescription &&
                            it.ownerId == testUser.id
                    },
                )
            }

            coVerify {
                libraryListRepository.insertItem(
                    item = match<LibraryListItem> {
                        it.listId == newListId &&
                            it.filmId == testMovie.identifier
                    },
                    film = testMovie,
                )
            }
        }

    @Test
    fun `seasonToDisplay should emit season data for tv shows`() =
        runTest(testDispatcher) {
            val testSeason = FilmTestDefaults.getSeason(
                episodes = listOf(FilmTestDefaults.getEpisode()),
            )
            val seasonWithProgress = SeasonWithProgress(
                season = testSeason,
                episodes = emptyList(),
            )

            every { getSeasonWithWatchProgress(any(), any()) } returns
                flowOf(Resource.Success(seasonWithProgress))

            createViewModel(testTvShow)
            advanceUntilIdle()

            viewModel.seasonToDisplay.test {
                expectThat(awaitItem()).isNotNull()
            }
        }

    @Test
    fun `watchProgress should emit user's watch progress for film`() =
        runTest(testDispatcher) {
            val episodeProgress = DatabaseTestDefaults.getEpisodeProgress(
                filmId = testTvShow.identifier,
                ownerId = testUser.id,
                status = WatchStatus.WATCHING,
            )
            val watchProgressWithMetadata = EpisodeProgressWithMetadata(episodeProgress, testTvShow.toDBFilm())

            every {
                watchProgressRepository.getAsFlow(
                    ownerId = testUser.id,
                    id = testTvShow.identifier,
                    type = testTvShow.filmType,
                )
            } returns flowOf(watchProgressWithMetadata)
            coEvery { getFilmMetadata(any()) } returns Resource.Success(testTvShow)

            createViewModel(testTvShow)
            advanceUntilIdle()

            viewModel.watchProgress.test {
                expectThat(awaitItem()).isEqualTo(episodeProgress)
            }
        }

    @Test
    fun `libraryLists should combine user lists with system lists`() =
        runTest(testDispatcher) {
            val userList = LibraryListWithItems(
                list = DatabaseTestDefaults.getLibraryList(name = "My List"),
                items = emptyList(),
            )

            every { libraryListRepository.getUserWithListsAndItems(testUser.id) } returns
                flowOf(UserWithLibraryListsAndItems(testUser, listOf(userList)))

            createViewModel()
            advanceUntilIdle()

            viewModel.libraryLists.test {
                val lists = awaitItem()
                expectThat(lists).hasSize(3) // User list + Watch Progress + Watchlist
            }
        }

    @Test
    fun `showFilmTitles should reflect datastore preferences`() =
        runTest(testDispatcher) {
            val uiPrefs = UiPreferences(shouldShowTitleOnCards = true)
            every { dataStoreManager.getUserPrefs(any<Preferences.Key<String>>(), UiPreferences::class) } returns
                flowOf(uiPrefs)

            createViewModel()
            advanceUntilIdle()

            viewModel.showFilmTitles.test {
                skipItems(1) // Skip initial value
                expectThat(awaitItem()).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `searchResults should filter lists based on query`() =
        runTest(testDispatcher) {
            turbineScope {
                val userList1 = LibraryListWithItems(
                    list = DatabaseTestDefaults.getLibraryList(id = 1, name = "Action Movies"),
                    items = emptyList(),
                )
                val userList2 = LibraryListWithItems(
                    list = DatabaseTestDefaults.getLibraryList(id = 2, name = "Comedy Shows"),
                    items = emptyList(),
                )

                every { libraryListRepository.getUserWithListsAndItems(testUser.id) } returns
                    flowOf(UserWithLibraryListsAndItems(testUser, listOf(userList1, userList2)))

                createViewModel()
                advanceUntilIdle()

                val librarySheetQueryTurbine = viewModel.librarySheetQuery.testIn(this)
                val libraryListsTurbine = viewModel.libraryLists.testIn(this)
                val searchResultsTurbine = viewModel.searchResults.testIn(this)

                viewModel.onLibrarySheetQueryChange("action")
                advanceUntilIdle()

                // Check query update
                librarySheetQueryTurbine.skipItems(1) // Skip initial state
                val query = librarySheetQueryTurbine.awaitItem()
                expectThat(query).isEqualTo("action")

                val lists = libraryListsTurbine.awaitItem()

                searchResultsTurbine.skipItems(1) // Skip initial empty query result
                val filteredResults = searchResultsTurbine.awaitItem()

                // Check lists and filtered results
                expectThat(lists).hasSize(4) // 2 user lists + watchlist + watch progress
                expectThat(filteredResults).hasSize(1)
                expectThat(filteredResults.first().list.name).isEqualTo("Action Movies")

                searchResultsTurbine.cancelAndIgnoreRemainingEvents()
                libraryListsTurbine.cancelAndIgnoreRemainingEvents()
                librarySheetQueryTurbine.cancelAndIgnoreRemainingEvents()
            }
        }
}
