package com.flixclusive.data.database.repository.impl

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@RunWith(AndroidJUnit4::class)
class LibraryListRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: LibraryListRepositoryImpl
    private lateinit var appDispatchers: AppDispatchers

    private val testDispatcher = StandardTestDispatcher()

    private val testUser = DatabaseTestDefaults.getUser()

    private val testLibraryList = DatabaseTestDefaults.getLibraryList()

    private val testFilm = DatabaseTestDefaults.getDBFilm()

    private val testLibraryItem = DatabaseTestDefaults.getLibraryListItem(filmId = testFilm.id)

    @Before
    fun setUp() {
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        database = DatabaseTestDefaults.createDatabase(
            context = ApplicationProvider.getApplicationContext(),
        )

        repository = LibraryListRepositoryImpl(
            listDao = database.libraryListDao(),
            itemDao = database.libraryListItemDao(),
            appDispatchers = appDispatchers,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun shouldInsertAndRetrieveLibraryList() =
        runTest(testDispatcher) {
            // Insert user first
            database.userDao().insert(testUser)

            // Insert library list
            val itemId = repository.insertList(testLibraryList)

            // Verify list was inserted
            repository.getList(itemId).test {
                val result = awaitItem()
                expectThat(result).isNotNull().and {
                    get { id }.isEqualTo(itemId)
                    get { name }.isEqualTo(testLibraryList.name)
                    get { description }.isEqualTo(testLibraryList.description)
                    get { ownerId }.isEqualTo(testLibraryList.ownerId)
                }
            }
        }

    @Test
    fun shouldRetrieveListsByUserId() =
        runTest(testDispatcher) {
            // Insert user first
            database.userDao().insert(testUser)

            // Insert multiple lists
            val list1 = testLibraryList.copy(id = 1, name = "List 1")
            val list2 = testLibraryList.copy(id = 2, name = "List 2")

            repository.insertList(list1)
            repository.insertList(list2)

            // Verify lists are retrieved
            repository.getLists(testUser.id).test {
                val result = awaitItem()
                expectThat(result).hasSize(2)
            }
        }

    @Test
    fun shouldUpdateLibraryList() =
        runTest(testDispatcher) {
            // Insert user first
            database.userDao().insert(testUser)

            // Insert original list
            val itemId = repository.insertList(testLibraryList)

            // Update list
            val updatedList = testLibraryList.copy(id = itemId, name = "Updated List Name")
            repository.updateList(updatedList)

            // Verify update
            repository.getList(itemId).test {
                val result = awaitItem()
                expectThat(result).isNotNull().get { name }.isEqualTo("Updated List Name")
            }
        }

    @Test
    fun shouldDeleteLibraryListById() =
        runTest(testDispatcher) {
            // Insert user first
            database.userDao().insert(testUser)

            // Insert list
            repository.insertList(testLibraryList)

            // Delete list
            repository.deleteListById(testLibraryList.id)

            // Verify deletion
            repository.getList(testLibraryList.id).test {
                val result = awaitItem()
                expectThat(result).isNull()
            }
        }

    @Test
    fun shouldInsertItem() =
        runTest(testDispatcher) {
            // Insert user and list
            database.userDao().insert(testUser)
            repository.insertList(testLibraryList)

            // Add item to list
            val id = repository.insertItem(testLibraryItem, testFilm)

            // Verify item was added
            repository.getItemAsFlow(id).test {
                expectThat(awaitItem()).isNotNull().and {
                    get { itemId }.isEqualTo(id)
                    get { filmId }.isEqualTo(testLibraryItem.filmId)
                }
            }
        }

    @Test
    fun shouldRetrieveListWithItems() =
        runTest(testDispatcher) {
            // Insert user and list
            database.userDao().insert(testUser)
            val itemId = repository.insertList(testLibraryList)

            // Add item to list
            repository.insertItem(testLibraryItem, testFilm)

            // Verify list with items
            repository.getListWithItems(itemId).test {
                val result = awaitItem()
                expectThat(result).isNotNull().and {
                    get { list.id }.isEqualTo(itemId)
                    get { items }.hasSize(1)
                }
            }
        }

    @Test
    fun shouldDeleteItem() =
        runTest(testDispatcher) {
            // Insert user and list
            database.userDao().insert(testUser)
            repository.insertList(testLibraryList)

            val itemId = repository.insertItem(testLibraryItem, testFilm)

            // Delete item from list
            repository.deleteItem(itemId)

            // Verify item deletion
            repository.getItemAsFlow(itemId).test {
                expectThat(awaitItem()).isNull()
            }
        }

    @Test
    fun shouldRetrieveItemWithLists() =
        runTest(testDispatcher) {
            // Insert user and lists
            database.userDao().insert(testUser)
            val list1 = testLibraryList.copy(name = "List 1")
            val list2 = testLibraryList.copy(name = "List 2")
            val id1 = repository.insertList(list1)
            val id2 = repository.insertList(list2)

            // Add item to both lists
            repository.insertItem(testLibraryItem.copy(listId = id1), testFilm)
            repository.insertItem(testLibraryItem.copy(listId = id2), testFilm)

            // Verify item with lists
            repository.getListsContainingFilm(testLibraryItem.filmId, testUser.id).test {
                expectThat(awaitItem()) {
                    isNotEmpty()
                    hasSize(2)
                }
            }
        }

    @Test
    fun shouldRetrieveUserWithListsAndItems() =
        runTest(testDispatcher) {
            // Insert user and list
            database.userDao().insert(testUser)
            repository.insertList(testLibraryList)
            repository.insertItem(testLibraryItem, testFilm)

            // Verify user with lists and items
            repository.getUserWithListsAndItems(testUser.id).test {
                val result = awaitItem()
                expectThat(result).and {
                    get { user.id }.isEqualTo(testUser.id)
                    get { lists }.hasSize(1)
                    get { lists.first().items }.hasSize(1)
                }
            }
        }
}
