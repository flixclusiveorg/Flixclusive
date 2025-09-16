package com.flixclusive.feature.mobile.library.details

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.library.LibraryListWithItems
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.core.database.entity.watchlist.WatchlistWithMetadata
import com.flixclusive.data.database.repository.LibraryListRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.feature.mobile.library.common.util.LibraryListUtil
import com.flixclusive.feature.mobile.library.common.util.LibrarySortFilter
import com.flixclusive.model.film.util.FilmType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryDetailsViewModelTest {
    private lateinit var viewModel: LibraryDetailsViewModel
    private lateinit var libraryListRepository: LibraryListRepository
    private lateinit var watchProgressRepository: WatchProgressRepository
    private lateinit var watchlistRepository: WatchlistRepository
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var context: Context
    private lateinit var savedStateHandle: SavedStateHandle

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    private val testLibrary = LibraryList(
        id = 1,
        ownerId = 1,
        name = "Test Library",
        description = "Test Description",
    )

    private val testDBFilm1 = DBFilm(
        id = "1",
        title = "Action Movie",
        year = 2020,
        rating = 8.5,
        filmType = FilmType.MOVIE,
    )

    private val testDBFilm2 = DBFilm(
        id = "2",
        title = "Comedy Show",
        year = 2021,
        rating = 7.2,
        filmType = FilmType.TV_SHOW,
    )

    private val testLibraryItem1 = LibraryListItem(
        id = 1L,
        filmId = "1",
        listId = 1,
        addedAt = Date(1000),
    )

    private val testLibraryItem2 = LibraryListItem(
        id = 2L,
        filmId = "2",
        listId = 1,
        addedAt = Date(2000),
    )

    private val testLibraryItemWithMetadata1 = LibraryListItemWithMetadata(
        item = testLibraryItem1,
        metadata = testDBFilm1,
    )

    private val testLibraryItemWithMetadata2 = LibraryListItemWithMetadata(
        item = testLibraryItem2,
        metadata = testDBFilm2,
    )

    private val testLibraryWithItems = LibraryListWithItems(
        list = testLibrary,
        items = listOf(testLibraryItemWithMetadata1, testLibraryItemWithMetadata2),
    )

    @Before
    fun setup() {
        libraryListRepository = mockk(relaxed = true)
        watchProgressRepository = mockk(relaxed = true)
        watchlistRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        appDispatchers = object : AppDispatchers {
            override val default: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher
            override val ioScope: CoroutineScope = CoroutineScope(testDispatcher)
            override val defaultScope: CoroutineScope = CoroutineScope(testDispatcher)
            override val mainScope: CoroutineScope = CoroutineScope(testDispatcher)
        }

        savedStateHandle = SavedStateHandle()
        savedStateHandle["library"] = testLibrary

        every { libraryListRepository.getListWithItems(any()) } returns flowOf(testLibraryWithItems)
    }

    private fun createViewModel() {
        viewModel = LibraryDetailsViewModel(
            context = context,
            libraryListRepository = libraryListRepository,
            watchProgressRepository = watchProgressRepository,
            watchlistRepository = watchlistRepository,
            appDispatchers = appDispatchers,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `initial state should be correct`() =
        runTest(testDispatcher) {
            createViewModel()

            expectThat(viewModel.uiState.value) {
                get { isLoading }.isFalse()
                get { isShowingSearchBar }.isFalse()
                get { isMultiSelecting }.isFalse()
                get { longClickedItem }.isEqualTo(null)
                get { selectedFilter }.isEqualTo(LibrarySortFilter.AddedAt)
                get { isSortingAscending }.isTrue()
            }

            expectThat(viewModel.searchQuery.value).isEqualTo("")
            expectThat(viewModel.selectedItems.value).isEqualTo(persistentSetOf())
        }

    @Test
    fun `library should emit the correct library data`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.library.test {
                expectThat(awaitItem()).isEqualTo(testLibrary)
            }
        }

    @Test
    fun `items should be sorted by addedAt ascending by default`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.items.test {
                val items = awaitItem()
                expectThat(items).hasSize(2)
                expectThat(items[0]).isEqualTo(testLibraryItemWithMetadata1) // Earlier date
                expectThat(items[1]).isEqualTo(testLibraryItemWithMetadata2) // Later date
            }
        }

    @Test
    fun `onUpdateFilter should toggle sort order when same filter is selected`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onUpdateFilter(LibrarySortFilter.AddedAt)

            expectThat(viewModel.uiState.value) {
                get { selectedFilter }.isEqualTo(LibrarySortFilter.AddedAt)
                get { isSortingAscending }.isFalse()
            }
        }

    @Test
    fun `onUpdateFilter should change filter and reset to ascending when different filter is selected`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onUpdateFilter(LibrarySortFilter.Name)

            expectThat(viewModel.uiState.value) {
                get { selectedFilter }.isEqualTo(LibrarySortFilter.Name)
                get { isSortingAscending }.isTrue()
            }
        }

    @Test
    fun `items should be sorted by name when name filter is applied`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onUpdateFilter(LibrarySortFilter.Name)

            viewModel.items.test {
                val items = awaitItem()
                expectThat(items).hasSize(2)
                expectThat(items[0].metadata.title).isEqualTo("Action Movie") // Alphabetically first
                expectThat(items[1].metadata.title).isEqualTo("Comedy Show")
            }
        }

    @Test
    fun `items should be sorted by rating when rating filter is applied`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onUpdateFilter(LibraryDetailsFilters.Rating)

            viewModel.items.test {
                val items = awaitItem()
                expectThat(items).hasSize(2)
                expectThat(items[0].metadata.rating).isEqualTo(7.2) // Lower rating first when ascending
                expectThat(items[1].metadata.rating).isEqualTo(8.5)
            }
        }

    @Test
    fun `items should be sorted by year when year filter is applied`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onUpdateFilter(LibraryDetailsFilters.Year)

            viewModel.items.test {
                val items = awaitItem()
                expectThat(items).hasSize(2)
                expectThat(items[0].metadata.year).isEqualTo(2020) // Earlier year first when ascending
                expectThat(items[1].metadata.year).isEqualTo(2021)
            }
        }

    @Test
    fun `search should filter items based on title`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onQueryChange("Action")
            advanceUntilIdle()

            viewModel.searchItems.test {
                skipItems(1) // Skip initial empty emission
                val searchResults = awaitItem()
                expectThat(searchResults).hasSize(1)
                expectThat(searchResults[0].metadata.title).isEqualTo("Action Movie")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `search should be case insensitive`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onQueryChange("action")
            advanceUntilIdle()

            viewModel.searchItems.test {
                skipItems(1)
                val searchResults = awaitItem()
                expectThat(searchResults).hasSize(1)
                expectThat(searchResults[0].metadata.title).isEqualTo("Action Movie")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `empty search query should not emit search results`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onQueryChange("")
            advanceUntilIdle()

            viewModel.searchItems.test {
                val searchResults = awaitItem()
                expectThat(searchResults).hasSize(0)
            }
        }

    @Test
    fun `onStartMultiSelecting should enable multi selection mode`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onStartMultiSelecting()

            expectThat(viewModel.uiState.value.isMultiSelecting).isTrue()
        }

    @Test
    fun `onToggleSelect should add item to selection when not selected`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onToggleSelect(testLibraryItemWithMetadata1)

            expectThat(viewModel.selectedItems.value).hasSize(1)
            expectThat(viewModel.selectedItems.value.contains(testLibraryItemWithMetadata1)).isTrue()
        }

    @Test
    fun `onToggleSelect should remove item from selection when already selected`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onToggleSelect(testLibraryItemWithMetadata1)
            viewModel.onToggleSelect(testLibraryItemWithMetadata1)

            expectThat(viewModel.selectedItems.value).hasSize(0)
        }

    @Test
    fun `onUnselectAll should clear selection and disable multi selection mode`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onStartMultiSelecting()
            viewModel.onToggleSelect(testLibraryItemWithMetadata1)
            viewModel.onUnselectAll()

            expectThat(viewModel.selectedItems.value).hasSize(0)
            expectThat(viewModel.uiState.value.isMultiSelecting).isFalse()
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
    fun `onLongClickItem should update long clicked item`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onLongClickItem(testLibraryItemWithMetadata1)

            expectThat(viewModel.uiState.value.longClickedItem).isEqualTo(testLibraryItemWithMetadata1)
        }

    @Test
    fun `onRemoveLongClickedItem should delete item from repository`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onLongClickItem(testLibraryItemWithMetadata1)
            viewModel.onRemoveLongClickedItem()
            advanceUntilIdle()

            coVerify { libraryListRepository.deleteItem(itemId = testLibraryItemWithMetadata1.itemId) }
        }

    @Test
    fun `onRemoveSelection should delete all selected items from repository`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onToggleSelect(testLibraryItemWithMetadata1)
            viewModel.onToggleSelect(testLibraryItemWithMetadata2)
            viewModel.onRemoveSelection()
            advanceUntilIdle()

            coVerify { libraryListRepository.deleteItem(itemId = testLibraryItemWithMetadata1.itemId) }
            coVerify { libraryListRepository.deleteItem(itemId = testLibraryItemWithMetadata2.itemId) }
        }

    @Test
    fun `watchlist library should use watchlist repository`() =
        runTest(testDispatcher) {
            val watchlistLibrary = testLibrary.copy(id = LibraryListUtil.WATCHLIST_LIB_ID)
            savedStateHandle["library"] = watchlistLibrary

            val mockWatchlistItem = mockk<WatchlistWithMetadata>(relaxed = true)
            every { watchlistRepository.getAllAsFlow(any()) } returns flowOf(listOf(mockWatchlistItem))
            every { context.getString(any()) } returns "Watchlist"

            createViewModel()

            coVerify { watchlistRepository.getAllAsFlow(watchlistLibrary.ownerId) }
        }

    @Test
    fun `watch progress library should use watch progress repository`() =
        runTest(testDispatcher) {
            val watchProgressLibrary = testLibrary.copy(id = LibraryListUtil.WATCH_PROGRESS_LIB_ID)
            savedStateHandle["library"] = watchProgressLibrary

            val mockWatchProgressItem = mockk<WatchProgressWithMetadata>(relaxed = true)
            every { watchProgressRepository.getAllAsFlow(any()) } returns flowOf(listOf(mockWatchProgressItem))
            every { context.getString(any()) } returns "Recently Watched"

            createViewModel()

            coVerify { watchProgressRepository.getAllAsFlow(watchProgressLibrary.ownerId) }
        }

    @Test
    fun `custom library should use library list repository`() =
        runTest(testDispatcher) {
            val customLibrary = testLibrary.copy(id = 5) // Any positive ID
            savedStateHandle["library"] = customLibrary

            createViewModel()

            coVerify { libraryListRepository.getListWithItems(listId = customLibrary.id) }
        }

    @Test
    fun `search should debounce query changes`() =
        runTest(testDispatcher) {
            createViewModel()

            turbineScope {
                val searchTurbine = viewModel.searchItems.testIn(this)

                viewModel.onQueryChange("A")
                viewModel.onQueryChange("Ac")
                viewModel.onQueryChange("Act")
                viewModel.onQueryChange("Action")

                // Only the final query should produce results after debounce
                advanceUntilIdle()

                searchTurbine.skipItems(1) // Skip initial empty emission
                val searchResults = searchTurbine.awaitItem()
                expectThat(searchResults).hasSize(1)
                expectThat(searchResults[0].metadata.title).isEqualTo("Action Movie")

                searchTurbine.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `multiple calls to onRemoveLongClickedItem should not start multiple jobs`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onLongClickItem(testLibraryItemWithMetadata1)

            // Call multiple times quickly
            viewModel.onRemoveLongClickedItem()
            viewModel.onRemoveLongClickedItem()
            viewModel.onRemoveLongClickedItem()

            advanceUntilIdle()

            // Should only be called once
            coVerify(exactly = 1) { libraryListRepository.deleteItem(itemId = testLibraryItemWithMetadata1.itemId) }
        }

    @Test
    fun `multiple calls to onRemoveSelection should not start multiple jobs`() =
        runTest(testDispatcher) {
            createViewModel()

            viewModel.onToggleSelect(testLibraryItemWithMetadata1)

            // Call multiple times quickly
            viewModel.onRemoveSelection()
            viewModel.onRemoveSelection()
            viewModel.onRemoveSelection()

            advanceUntilIdle()

            // Should only be called once per item
            coVerify(exactly = 1) { libraryListRepository.deleteItem(itemId = testLibraryItemWithMetadata1.itemId) }
        }
}
