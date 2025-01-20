package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.LibraryListDao
import com.flixclusive.model.database.LibraryItemId
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LibraryListDaoTest {
    private val commonListId = UUID.randomUUID().toString()

    private lateinit var dao: LibraryListDao
    private lateinit var db: AppDatabase


    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context = context,
            klass = AppDatabase::class.java
        ).build()
        dao = db.libraryListDao()

        runBlocking {
            dao.createList(
                LibraryList(
                    id = commonListId,
                    ownerId = 1,
                    name = "Sample custom list"
                )
            )
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testCreateList() = runTest {
        val listName = "Horror catalogue"
        val list = LibraryList(
            id = UUID.randomUUID().toString(),
            ownerId = 1,
            name = listName
        )

        dao.createList(list)
        val queriedList = dao.getLibraryListAsFlow(list.id).first()

        assert(queriedList?.id == list.id)
        assert(queriedList?.name == listName)
        assert(queriedList?.ownerId == 1)
    }

    @Test
    @Throws(Exception::class)
    fun testAddingItemToList() = runTest {
        val itemId = LibraryItemId(
            providerId = "TMDB",
            itemId = "tt20281892"
        )
        val item = LibraryListItem(
            entryId = 0,
            listId = commonListId,
            libraryItemId = itemId
        )

        dao.addItemToList(item)
        val list = dao.getListItemsAsFlow(commonListId).first()
        assert(list.size == 1)
        assert(list.first().listId == commonListId)
        assert(list.first().libraryItemId == itemId)
    }

    @Test
    @Throws(Exception::class)
    fun testRemovingItemToList() = runTest {
        val itemId = LibraryItemId(
            providerId = "TMDB",
            itemId = "tt20281892"
        )
        val item = LibraryListItem(
            entryId = 0,
            listId = commonListId,
            libraryItemId = itemId
        )

        dao.removeItemFromList(item)
        val list = dao.getListItemsAsFlow(commonListId).first()
        assert(list.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testRemovingList() = runTest {
        val listName = "Horror catalogue"
        val list = LibraryList(
            id = UUID.randomUUID().toString(),
            ownerId = 1,
            name = listName
        )

        dao.createList(list)
        dao.removeList(list)
        val queriedList = dao.getLibraryListAsFlow(list.id).first()

        assert(queriedList == null)
    }

    @Test
    @Throws(Exception::class)
    fun testGettingLibraryListsByOwnerId() = runTest {
        val listName = "Catalog"
        val listsSize = 50
        repeat(listsSize) {
            dao.createList(
                LibraryList(
                    id = UUID.randomUUID().toString(),
                    ownerId = 1,
                    name = "$listName #$it"
                )
            )
        }

        val list = dao.getLibraryListsAsFlow(1).first()
        assert(list.size >= listsSize)
    }

    @Test
    @Throws(Exception::class)
    fun testGettingItemsByListId() = runTest {
        val listsSize = 50
        repeat(listsSize) {
            val itemId = LibraryItemId(
                providerId = "TMDB",
                itemId = "tt20281892-$it"
            )

            dao.addItemToList(
                LibraryListItem(
                    entryId = 0,
                    listId = commonListId,
                    libraryItemId = itemId
                )
            )
        }

        val list = dao.getListItemsAsFlow(commonListId).first()
        assert(list.size >= listsSize)
    }

    @Test
    @Throws(Exception::class)
    fun testAddItemAndRead() = runTest {
        val itemId = LibraryItemId(
            providerId = "TMDB",
            itemId = "tt20281892"
        )

        dao.addItemToList(
            LibraryListItem(
                entryId = 0,
                listId = commonListId,
                libraryItemId = itemId
            )
        )

        val item = dao.getListItem(0)
        val itemAsFlow = dao.getListItemAsFlow(0).first()
        assert(item != null)
        assert(itemAsFlow != null)
    }
}
