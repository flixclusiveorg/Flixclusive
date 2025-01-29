package com.flixclusive.data.library.custom

import com.flixclusive.model.database.DBFilm
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class DefaultLibraryListRepositoryTest {
    private lateinit var repository: LibraryListRepository
    private lateinit var fakeDataSource: FakeLibraryListDataSource

    @Before
    fun setup() {
        fakeDataSource = FakeLibraryListDataSource()
        repository = DefaultLibraryListRepository(fakeDataSource)
    }

    @Test
    fun `insert and retrieve list`() =
        runTest {
            val testList = createTestLibraryList(1)
            repository.insertList(testList)

            val lists = repository.getLists(1).first()
            assertEquals(1, lists.size)
            assertEquals(testList, lists[0])
        }

    @Test
    fun `update list`() =
        runTest {
            val originalList = createTestLibraryList(1)
            repository.insertList(originalList)

            val updatedList = originalList.copy(name = "Updated Name")
            repository.updateList(updatedList)

            val retrieved = repository.getList(1).first()
            assertEquals("Updated Name", retrieved?.name)
        }

    @Test
    fun `delete list`() =
        runTest {
            val testList = createTestLibraryList(1)
            repository.insertList(testList)
            repository.deleteListById(testList.id)

            val lists = repository.getLists(1).first()
            assertTrue(lists.isEmpty())
        }

    @Test
    fun `add item to list`() =
        runTest {
            val testList = createTestLibraryList(1)
            val testItem = createTestListItem("item1")

            repository.insertList(testList)
            repository.addItemToList(testList.id, testItem)

            val listWithItems = repository.getListWithItems(1).first()
            assertEquals(1, listWithItems?.items?.size)
            assertEquals("item1", listWithItems?.items?.get(0)?.id)
        }

    @Test
    fun `remove item from list`() =
        runTest {
            val testList = createTestLibraryList(1)
            val testItem = createTestListItem("item1")

            repository.insertList(testList)
            repository.addItemToList(testList.id, testItem)
            repository.deleteItemFromList(1, "item1")

            val listWithItems = repository.getListWithItems(1).first()
            assertTrue(listWithItems?.items?.isEmpty() ?: false)
        }

    @Test
    fun `get user with lists and items`() =
        runTest {
            val testList = createTestLibraryList(1)
            val testItem = createTestListItem("item1")

            repository.insertList(testList)
            repository.addItemToList(testList.id, testItem)

            val userWithData = repository.getUserWithListsAndItems(1).first()
            assertEquals(1, userWithData.list.size)
            assertEquals(
                1,
                userWithData
                    .list[0]
                    .items
                    .size,
            )
        }

    // Helper functions
    private fun createTestLibraryList(id: Int) =
        LibraryList(
            id = id,
            ownerId = 1,
            name = "Test List",
            description = "Test Description",
        )

    private fun createTestListItem(id: String) =
        LibraryListItem(
            id = id,
            film =
                DBFilm(
                    id = id,
                    title = "Test Film",
                    posterImage = "/test.jpg",
                    providerId = DEFAULT_FILM_SOURCE_NAME,
                    releaseDate = "2024-01-01",
                ),
        )
}
