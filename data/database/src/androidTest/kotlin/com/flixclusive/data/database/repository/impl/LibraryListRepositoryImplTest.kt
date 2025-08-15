package com.flixclusive.data.database.repository.impl

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.data.database.datasource.impl.LocalLibraryListDataSource
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@RunWith(AndroidJUnit4::class)
class LibraryListRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: LibraryListRepositoryImpl

    private val testUser = DatabaseTestDefaults.getUser()

    private val testLibraryList = DatabaseTestDefaults.getLibraryList()

    private val testLibraryItem = DatabaseTestDefaults.getLibraryListItem()

    @Before
    fun setUp() {
        database = DatabaseTestDefaults.createDatabase(
            context = ApplicationProvider.getApplicationContext(),
        )
        val dataSource = LocalLibraryListDataSource(
            listDao = database.libraryListDao(),
            itemDao = database.libraryListItemDao(),
            crossRefDao = database.libraryListCrossRefDao(),
        )
        repository = LibraryListRepositoryImpl(dataSource)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun shouldInsertAndRetrieveLibraryList() =
        runTest {
            // Insert user first
            database.userDao().insert(testUser)

            // Insert library list
            repository.insertList(testLibraryList)

            // Verify list was inserted
            repository.getList(testLibraryList.id).test {
                val result = awaitItem()
                expectThat(result).isNotNull().and {
                    get { id }.isEqualTo(testLibraryList.id)
                    get { name }.isEqualTo(testLibraryList.name)
                    get { description }.isEqualTo(testLibraryList.description)
                    get { ownerId }.isEqualTo(testLibraryList.ownerId)
                }
            }
        }

    @Test
    fun shouldRetrieveListsByUserId() =
        runTest {
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
        runTest {
            // Insert user first
            database.userDao().insert(testUser)

            // Insert original list
            repository.insertList(testLibraryList)

            // Update list
            val updatedList = testLibraryList.copy(name = "Updated List Name")
            repository.updateList(updatedList)

            // Verify update
            repository.getList(testLibraryList.id).test {
                val result = awaitItem()
                expectThat(result).isNotNull().get { name }.isEqualTo("Updated List Name")
            }
        }

    @Test
    fun shouldDeleteLibraryListById() =
        runTest {
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
    fun shouldAddItemToList() =
        runTest {
            // Insert user and list
            database.userDao().insert(testUser)
            repository.insertList(testLibraryList)

            // Add item to list
            repository.addItemToList(testLibraryList.id, testLibraryItem)

            // Verify item was added
            repository.getItem(testLibraryItem.id).test {
                val result = awaitItem()
                expectThat(result).isNotNull().and {
                    get { id }.isEqualTo(testLibraryItem.id)
                    get { film.title }.isEqualTo(testLibraryItem.film.title)
                }
            }
        }

    @Test
    fun shouldRetrieveListWithItems() =
        runTest {
            // Insert user and list
            database.userDao().insert(testUser)
            repository.insertList(testLibraryList)

            // Add item to list
            repository.addItemToList(testLibraryList.id, testLibraryItem)

            // Verify list with items
            repository.getListWithItems(testLibraryList.id).test {
                val result = awaitItem()
                expectThat(result).isNotNull().and {
                    get { list.id }.isEqualTo(testLibraryList.id)
                    get { items }.hasSize(1)
                }
            }
        }

    @Test
    fun shouldUpdateLibraryItem() =
        runTest {
            // Insert user and list
            database.userDao().insert(testUser)
            repository.insertList(testLibraryList)
            repository.addItemToList(testLibraryList.id, testLibraryItem)

            // Update item
            val updatedItem = DatabaseTestDefaults.getLibraryListItem(
                film = testLibraryItem.film.copy(title = "Updated Title"),
            )
            repository.updateItem(updatedItem)

            // Verify update
            repository.getItem(testLibraryItem.id).test {
                val result = awaitItem()
                expectThat(result) {
                    isNotNull()
                    get { this!!.film.title }.isEqualTo("Updated Title")
                }
            }
        }

    @Test
    fun shouldDeleteItemFromList() =
        runTest {
            // Insert user and list
            database.userDao().insert(testUser)
            val list1 = testLibraryList.copy(id = 1, name = "List 1")
            val list2 = testLibraryList.copy(id = 2, name = "List 2")
            repository.insertList(list1)
            repository.insertList(list2)
            repository.addItemToList(list1.id, testLibraryItem)
            repository.addItemToList(list2.id, testLibraryItem)

            // Delete item from list
            repository.deleteItemFromList(list1.id, testLibraryItem.id)

            // Verify item still exists but not in list
            repository.getItem(testLibraryItem.id).test {
                val result = awaitItem()
                expectThat(result).isNotNull()
            }

            // Verify cross reference was deleted
            repository.getCrossRef(list1.id, testLibraryItem.id).test {
                val result = awaitItem()
                expectThat(result).isNull()
            }
        }

    @Test
    fun shouldRetrieveItemWithLists() =
        runTest {
            // Insert user and lists
            database.userDao().insert(testUser)
            val list1 = testLibraryList.copy(id = 1, name = "List 1")
            val list2 = testLibraryList.copy(id = 2, name = "List 2")
            repository.insertList(list1)
            repository.insertList(list2)

            // Add item to both lists
            repository.addItemToList(list1.id, testLibraryItem)
            repository.addItemToList(list2.id, testLibraryItem)

            // Verify item with lists
            repository.getItemWithLists(testLibraryItem.id).test {
                val result = awaitItem()
                expectThat(result).isNotNull().and {
                    get { item.id }.isEqualTo(testLibraryItem.id)
                    get { lists }.hasSize(2)
                }
            }
        }

    @Test
    fun shouldRetrieveUserWithListsAndItems() =
        runTest {
            // Insert user and list
            database.userDao().insert(testUser)
            repository.insertList(testLibraryList)
            repository.addItemToList(testLibraryList.id, testLibraryItem)

            // Verify user with lists and items
            repository.getUserWithListsAndItems(testUser.id).test {
                val result = awaitItem()
                expectThat(result).and {
                    get { user.id }.isEqualTo(testUser.id)
                    get { list }.hasSize(1)
                    get { list.first().items }.hasSize(1)
                }
            }
        }
}
