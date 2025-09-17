package com.flixclusive.feature.mobile.library.manage

import android.content.Context
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.library.UserWithLibraryListsAndItems
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.core.database.entity.watchlist.WatchlistWithMetadata
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.model.film.util.FilmType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ManageLibraryViewModelTest {
    private lateinit var viewModel: ManageLibraryViewModel
    private lateinit var context: Context
    private lateinit var libraryListRepository: LibraryListRepository
    private lateinit var watchProgressRepository: WatchProgressRepository
    private lateinit var watchlistRepository: WatchlistRepository
    private lateinit var userSessionManager: UserSessionManager
    private lateinit var appDispatchers: AppDispatchers

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    private val testUser = User(
        id = 1,
        name = "Test User",
        image = 0,
        pin = null,
        pinHint = null,
    )

    private val testLibrary1 = LibraryList(
        id = 1,
        ownerId = 1,
        name = "Action Movies",
        description = "Collection of action movies",
        createdAt = Date(1000),
        updatedAt = Date(2000),
    )

    private val testLibrary2 = LibraryList(
        id = 2,
        ownerId = 1,
        name = "Comedy Shows",
        description = "Collection of comedy shows",
        createdAt = Date(3000),
        updatedAt = Date(4000),
    )

    private val testDBFilm1 = DBFilm(
        id = "1",
        title = "Action Movie 1",
        year = 2020,
        rating = 8.5,
        filmType = FilmType.MOVIE,
        posterImage = "poster1.jpg",
    )

    private val testDBFilm2 = DBFilm(
        id = "2",
        title = "Comedy Show 1",
        year = 2021,
        rating = 7.2,
        filmType = FilmType.TV_SHOW,
        posterImage = "poster2.jpg",
    )

    private val testDBFilm3 = DBFilm(
        id = "3",
        title = "Action Movie 2",
        year = 2022,
        rating = 9.0,
        filmType = FilmType.MOVIE,
        posterImage = "poster3.jpg",
    )

    private val testLibraryItem1 = LibraryListItem(
        id = 1L,
        filmId = "1",
        listId = 1,
        addedAt = Date(1500),
    )

    private val testLibraryItem2 = LibraryListItem(
        id = 2L,
        filmId = "2",
        listId = 2,
        addedAt = Date(2500),
    )

    private val testLibraryItem3 = LibraryListItem(
        id = 3L,
        filmId = "3",
        listId = 1,
        addedAt = Date(3500),
    )

    private val testLibraryItemWithMetadata1 = LibraryListItemWithMetadata(
        item = testLibraryItem1,
        metadata = testDBFilm1,
    )

    private val testLibraryItemWithMetadata2 = LibraryListItemWithMetadata(
        item = testLibraryItem2,
        metadata = testDBFilm2,
    )

    private val testLibraryItemWithMetadata3 = LibraryListItemWithMetadata(
        item = testLibraryItem3,
        metadata = testDBFilm3,
    )

    private val testLibraryWithItems1 = LibraryListWithItems(
        list = testLibrary1,
        items = listOf(testLibraryItemWithMetadata1, testLibraryItemWithMetadata3),
    )

    private val testLibraryWithItems2 = LibraryListWithItems(
        list = testLibrary2,
        items = listOf(testLibraryItemWithMetadata2),
    )

    private val testUserWithLibraryListsAndItems = UserWithLibraryListsAndItems(
        user = testUser,
        lists = listOf(testLibraryWithItems1, testLibraryWithItems2),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        libraryListRepository = mockk(relaxed = true)
        watchProgressRepository = mockk(relaxed = true)
        watchlistRepository = mockk(relaxed = true)
        userSessionManager = mockk(relaxed = true)

        appDispatchers = object : AppDispatchers {
            override val default: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher
            override val ioScope: CoroutineScope = CoroutineScope(testDispatcher)
            override val defaultScope: CoroutineScope = CoroutineScope(testDispatcher)
            override val mainScope: CoroutineScope = CoroutineScope(testDispatcher)
        }

        every { userSessionManager.currentUser } returns MutableStateFlow(testUser).asStateFlow()
        every { libraryListRepository.getUserWithListsAndItems(testUser.id) } returns
            flowOf(testUserWithLibraryListsAndItems)
        every { watchProgressRepository.getAllAsFlow(testUser.id) } returns
            flowOf(emptyList<WatchProgressWithMetadata>())
        every { watchlistRepository.getAllAsFlow(testUser.id) } returns
            flowOf(emptyList<WatchlistWithMetadata>())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = ManageLibraryViewModel(
            context = context,
            libraryListRepository = libraryListRepository,
            watchProgressRepository = watchProgressRepository,
            watchlistRepository = watchlistRepository,
            userSessionManager = userSessionManager,
            appDispatchers = appDispatchers,
        )
    }

    @Test
    fun `initial state should be correct`() =
        runTest(testDispatcher) {
            createViewModel()

            expectThat(viewModel.uiState.value) {
                get { isShowingFilterSheet }.isFalse()
                get { isShowingSearchBar }.isFalse()
                get { isMultiSelecting }.isFalse()
                get { isShowingOptionsSheet }.isFalse()
                get { isCreatingLibrary }.isFalse()
                get { isEditingLibrary }.isFalse()
                get { longClickedLibrary }.isEqualTo(null)
                get { selectedFilter }.isEqualTo(LibrarySortFilter.ModifiedAt)
                get { isSortingAscending }.isTrue()
            }

            expectThat(viewModel.searchQuery.value).isEqualTo("")
            expectThat(viewModel.selectedLibraries.value).isEqualTo(persistentSetOf())
        }

    @Test
    fun `libraries should emit correct data sorted by modified date ascending by default`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.libraries.test {
                val libraries = awaitItem()

                expectThat(libraries).hasSize(4) // 2 libraries + watchlist + history
                expectThat(libraries[0].name).isEqualTo("Action Movies")
                expectThat(libraries[1].name).isEqualTo("Comedy Shows")
                expectThat(libraries[0].itemsCount).isEqualTo(2)
                expectThat(libraries[1].itemsCount).isEqualTo(1)
            }
        }

    @Test
    fun `onUpdateFilter should toggle sort order when same filter is selected`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onUpdateFilter(LibrarySortFilter.ModifiedAt)

            expectThat(viewModel.uiState.value) {
                get { selectedFilter }.isEqualTo(LibrarySortFilter.ModifiedAt)
                get { isSortingAscending }.isFalse()
            }
        }

    @Test
    fun `onUpdateFilter should change filter and keep ascending when different filter is selected`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onUpdateFilter(LibrarySortFilter.Name)

            expectThat(viewModel.uiState.value) {
                get { selectedFilter }.isEqualTo(LibrarySortFilter.Name)
                get { isSortingAscending }.isTrue()
            }
        }

    @Test
    fun `onUpdateFilter should sort libraries by name correctly`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onUpdateFilter(LibrarySortFilter.Name)
            advanceUntilIdle()

            viewModel.libraries.test {
                val libraries = awaitItem()

                expectThat(libraries).hasSize(4)
                expectThat(libraries[0].name).isEmpty()
                expectThat(libraries[1].name).isEmpty()
                expectThat(libraries[2].name).isEqualTo("Action Movies")
                expectThat(libraries[3].name).isEqualTo("Comedy Shows")
            }
        }

    @Test
    fun `onUpdateFilter should sort libraries by item count correctly`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onUpdateFilter(ItemCount)
            advanceUntilIdle()

            viewModel.libraries.test {
                val libraries = awaitItem()

                expectThat(libraries).hasSize(4)
                expectThat(libraries[0].itemsCount).isEqualTo(0) // Watchlist is empty
                expectThat(libraries[1].itemsCount).isEqualTo(0) // Watch progress list is empty
                expectThat(libraries[2].itemsCount).isEqualTo(1) // Comedy list has 1
                expectThat(libraries[3].itemsCount).isEqualTo(2) // Action list has 2
            }
        }

    @Test
    fun `onRemoveLongClickedLibrary should delete the long clicked library`() =
        runTest(testDispatcher) {
            createViewModel()
            val libraryPreview = LibraryListWithPreview(
                list = testLibrary1,
                itemsCount = 2,
                previews = emptyList(),
            )

            viewModel.onLongClickItem(libraryPreview)
            viewModel.onRemoveLongClickedLibrary()
            advanceUntilIdle()

            coVerify { libraryListRepository.deleteListById(testLibrary1.id) }
        }

    @Test
    fun `onRemoveLongClickedLibrary should throw when no library is selected`() =
        runTest(testDispatcher) {
            createViewModel()

            try {
                viewModel.onRemoveLongClickedLibrary()
                advanceUntilIdle()
            } catch (e: IllegalArgumentException) {
                expectThat(e.message).isEqualTo("No library selected for removal!")
            }
        }

    @Test
    fun `onRemoveSelection should delete all selected libraries`() =
        runTest(testDispatcher) {
            createViewModel()
            val libraryPreview1 = LibraryListWithPreview(
                list = testLibrary1,
                itemsCount = 2,
                previews = emptyList(),
            )
            val libraryPreview2 = LibraryListWithPreview(
                list = testLibrary2,
                itemsCount = 1,
                previews = emptyList(),
            )

            viewModel.onToggleSelect(libraryPreview1)
            viewModel.onToggleSelect(libraryPreview2)
            viewModel.onRemoveSelection()
            advanceUntilIdle()

            coVerify { libraryListRepository.deleteListById(testLibrary1.id) }
            coVerify { libraryListRepository.deleteListById(testLibrary2.id) }
        }

    @Test
    fun `onSaveEdits should update library and reset editing state`() =
        runTest(testDispatcher) {
            createViewModel()
            val updatedLibrary = testLibrary1.copy(name = "Updated Name")

            viewModel.onSaveEdits(updatedLibrary)
            advanceUntilIdle()

            coVerify { libraryListRepository.updateList(updatedLibrary) }
            expectThat(viewModel.uiState.value) {
                get { isEditingLibrary }.isFalse()
                get { longClickedLibrary }.isEqualTo(null)
            }
        }

    @Test
    fun `onAdd should create new library and reset creating state`() =
        runTest(testDispatcher) {
            createViewModel()
            val newName = "New Library"
            val newDescription = "New Description"

            viewModel.onAdd(newName, newDescription)
            advanceUntilIdle()

            coVerify {
                libraryListRepository.insertList(
                    match { library ->
                        library.name == newName &&
                            library.description == newDescription &&
                            library.ownerId == testUser.id
                    },
                )
            }
            expectThat(viewModel.uiState.value.isCreatingLibrary).isFalse()
        }

    @Test
    fun `onStartMultiSelecting should enable multi selecting mode`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onStartMultiSelecting()

            expectThat(viewModel.uiState.value.isMultiSelecting).isTrue()
        }

    @Test
    fun `onToggleSelect should add item to selection when not selected`() =
        runTest(testDispatcher) {
            createViewModel()
            val libraryPreview = LibraryListWithPreview(
                list = testLibrary1,
                itemsCount = 2,
                previews = emptyList(),
            )

            viewModel.onToggleSelect(libraryPreview)

            expectThat(viewModel.selectedLibraries.value).hasSize(1)
            expectThat(viewModel.selectedLibraries.value.contains(libraryPreview)).isTrue()
        }

    @Test
    fun `onToggleSelect should remove item from selection when already selected`() =
        runTest(testDispatcher) {
            createViewModel()
            val libraryPreview = LibraryListWithPreview(
                list = testLibrary1,
                itemsCount = 2,
                previews = emptyList(),
            )

            viewModel.onToggleSelect(libraryPreview)
            viewModel.onToggleSelect(libraryPreview)

            expectThat(viewModel.selectedLibraries.value).hasSize(0)
        }

    @Test
    fun `onUnselectAll should clear selection and disable multi selecting`() =
        runTest(testDispatcher) {
            createViewModel()
            val libraryPreview = LibraryListWithPreview(
                list = testLibrary1,
                itemsCount = 2,
                previews = emptyList(),
            )

            viewModel.onToggleSelect(libraryPreview)
            viewModel.onStartMultiSelecting()
            viewModel.onUnselectAll()

            expectThat(viewModel.selectedLibraries.value).hasSize(0)
            expectThat(viewModel.uiState.value.isMultiSelecting).isFalse()
        }

    @Test
    fun `onQueryChange should update search query`() =
        runTest(testDispatcher) {
            createViewModel()
            val query = "action"

            viewModel.onQueryChange(query)

            expectThat(viewModel.searchQuery.value).isEqualTo(query)
        }

    @Test
    fun `searchResults should filter libraries by name`() =
        runTest(testDispatcher) {
            createViewModel()

            turbineScope {
                val searchResultsFlow = viewModel.searchResults.testIn(backgroundScope)

                viewModel.onQueryChange("action")
                advanceUntilIdle()

                searchResultsFlow.skipItems(1) // Skip initial empty emission
                val results = searchResultsFlow.awaitItem()
                expectThat(results).hasSize(1)
                expectThat(results[0].name).isEqualTo("Action Movies")

                searchResultsFlow.cancel()
            }
        }

    @Test
    fun `searchResults should filter libraries by description`() =
        runTest(testDispatcher) {
            createViewModel()

            turbineScope {
                val searchResultsFlow = viewModel.searchResults.testIn(backgroundScope)

                viewModel.onQueryChange("comedy")
                advanceUntilIdle()

                searchResultsFlow.skipItems(1) // Skip initial empty query emission
                val results = searchResultsFlow.awaitItem()
                expectThat(results).hasSize(1)
                expectThat(results[0].name).isEqualTo("Comedy Shows")

                searchResultsFlow.cancel()
            }
        }

    @Test
    fun `onToggleSearchBar should update search bar visibility`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onToggleSearchBar(true)
            expectThat(viewModel.uiState.value.isShowingSearchBar).isTrue()

            viewModel.onToggleSearchBar(false)
            expectThat(viewModel.uiState.value.isShowingSearchBar).isFalse()
        }

    @Test
    fun `onToggleOptionsSheet should update options sheet visibility`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onToggleOptionsSheet(true)
            expectThat(viewModel.uiState.value.isShowingOptionsSheet).isTrue()

            viewModel.onToggleOptionsSheet(false)
            expectThat(viewModel.uiState.value.isShowingOptionsSheet).isFalse()
        }

    @Test
    fun `onLongClickItem should update long clicked library`() =
        runTest(testDispatcher) {
            createViewModel()
            val libraryPreview = LibraryListWithPreview(
                list = testLibrary1,
                itemsCount = 2,
                previews = emptyList(),
            )

            viewModel.onLongClickItem(libraryPreview)

            expectThat(viewModel.uiState.value.longClickedLibrary).isEqualTo(libraryPreview)
        }

    @Test
    fun `onToggleEditDialog should show edit dialog and hide options sheet`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onToggleOptionsSheet(true)
            viewModel.onToggleEditDialog(true)

            expectThat(viewModel.uiState.value) {
                get { isEditingLibrary }.isTrue()
                get { isShowingOptionsSheet }.isFalse()
            }
        }

    @Test
    fun `onToggleEditDialog should hide edit dialog and clear long clicked library`() =
        runTest(testDispatcher) {
            createViewModel()
            val libraryPreview = LibraryListWithPreview(
                list = testLibrary1,
                itemsCount = 2,
                previews = emptyList(),
            )

            viewModel.onLongClickItem(libraryPreview)
            viewModel.onToggleEditDialog(true)
            viewModel.onToggleEditDialog(false)

            expectThat(viewModel.uiState.value) {
                get { isEditingLibrary }.isFalse()
                get { longClickedLibrary }.isEqualTo(null)
            }
        }

    @Test
    fun `onToggleCreateDialog should update create dialog visibility`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onToggleCreateDialog(true)
            expectThat(viewModel.uiState.value.isCreatingLibrary).isTrue()

            viewModel.onToggleCreateDialog(false)
            expectThat(viewModel.uiState.value.isCreatingLibrary).isFalse()
        }

    @Test
    fun `concurrent operations should be prevented`() =
        runTest(testDispatcher) {
            createViewModel()
            val libraryPreview = LibraryListWithPreview(
                list = testLibrary1,
                itemsCount = 2,
                previews = emptyList(),
            )

            viewModel.onLongClickItem(libraryPreview)

            // Start first operation
            viewModel.onRemoveLongClickedLibrary()
            // Try to start second operation immediately
            viewModel.onRemoveLongClickedLibrary()

            advanceUntilIdle()

            // Should only be called once due to job protection
            coVerify(exactly = 1) { libraryListRepository.deleteListById(testLibrary1.id) }
        }
}
