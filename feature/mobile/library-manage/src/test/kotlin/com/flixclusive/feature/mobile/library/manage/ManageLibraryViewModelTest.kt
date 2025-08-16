package com.flixclusive.feature.mobile.library.manage

import com.flixclusive.core.database.entity.DBFilm
import com.flixclusive.core.database.entity.LibraryList
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

const val DEFAULT_OWNER_ID = 1
val fakeUser = User(
    id = DEFAULT_OWNER_ID,
    name = "Test",
    image = 1,
    pin = "",
    pinHint = null
)

class ManageLibraryViewModelTest {
    private lateinit var viewModel: ManageLibraryViewModel
    private lateinit var libraryListRepository: FakeLibraryListRepository

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        libraryListRepository = FakeLibraryListRepository()

        viewModel = ManageLibraryViewModel(
            libraryListRepository = libraryListRepository,
            userSessionManager = mockk {
                every { currentUser } returns MutableStateFlow(fakeUser).asStateFlow()
            }
        )
    }

    @Test
    fun `updating filter should change ui state filter`() = runTest {
        val testFilter = LibrarySortFilter.Name

        viewModel.onUpdateFilter(testFilter)

        val uiState = getCurrentUiState()
        assertEquals(uiState.selectedFilter, testFilter)
    }

    @Test
    fun `updating filter direction should toggle current direction`() = runTest {
        var uiState = getCurrentUiState()
        val currentFilter = uiState.selectedFilter
        val currentFilterDirection = uiState.selectedFilterDirection

        viewModel.onUpdateFilter(currentFilter)

        uiState = getCurrentUiState()
        assertEquals(uiState.selectedFilterDirection, currentFilterDirection.toggle())
    }

    @Test
    fun `changing filters should update libraries order`() = runTest {
        viewModel.onUpdateFilter(LibrarySortFilter.Name)
        val ascendingDirection = getCurrentUiState().selectedFilterDirection
        var libraries = getCurrentLibraries()
        var firstItem = libraries.first().list
        var lastItem = libraries.last().list

        assertEquals(firstItem.name, getLibraryName(0))
        assertEquals(lastItem.name, getLibraryName(MAX_SIZE_LIST - 1))

        viewModel.onUpdateFilter(LibrarySortFilter.Name)
        libraries = getCurrentLibraries()
        firstItem = libraries.first().list
        lastItem = libraries.last().list

        assertEquals(ascendingDirection.toggle(), getCurrentUiState().selectedFilterDirection)
        assertEquals(firstItem.name, getLibraryName(MAX_SIZE_LIST - 1))
        assertEquals(lastItem.name, getLibraryName(0))
    }

    @Test
    fun `adding item should update libraries`() = runTest {
        val newId = MAX_SIZE_LIST + 1
        val newList = LibraryList(
            id = newId,
            name = getLibraryName(newId),
            ownerId = DEFAULT_OWNER_ID
        )

        libraryListRepository.insertList(newList)
        val libraries = getCurrentLibraries()
        assertEquals(libraries.size, newId)
    }

    @Test
    fun `removing item should update libraries`() = runTest {
        libraryListRepository.deleteListById(MAX_SIZE_LIST)
        val libraries = getCurrentLibraries()
        assertEquals(libraries.size, MAX_SIZE_LIST - 1)
    }

    @Test
    fun `updating item should update libraries`() = runTest {
        val firstList = getCurrentLibraries().first().list

        val newList = firstList.copy(name = "Test")
        libraryListRepository.updateList(newList)

        val libraries = libraryListRepository.getLists(fakeUser.id).first()
        assertEquals(libraries.first(), newList)
    }

    @Test
    fun `removing items selected should update libraries`() = runTest {
        val sizeToRemove = 3
        libraryListRepository.getLists(fakeUser.id).first().take(sizeToRemove).forEach {
            viewModel.onToggleSelect(
                LibraryListWithPreview(
                    list = it,
                    itemsCount = 20,
                    previews = List(3) { DBFilm().toPreviewPoster() }
                )
            )
        }

        val uiState = getCurrentUiState()
        val selectedLibraries = uiState.selectedLibraries
        selectedLibraries.forEach {
            libraryListRepository.deleteListById(it.list.id)
        }

        val libraries = getCurrentLibraries()
        assertEquals(libraries.size, MAX_SIZE_LIST - sizeToRemove)
    }

    private suspend fun getCurrentLibraries() = viewModel.libraries.first()
    private suspend fun getCurrentUiState() = viewModel.uiState.first()
    private fun getLibraryName(count: Int) = String.format(Locale.ROOT, LIBRARY_FORMAT_NAME, count)
}
