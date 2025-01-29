package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.LibraryListDao
import com.flixclusive.model.database.LibraryList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class LibraryListDaoTest {
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
            id = 1,
            ownerId = 1,
            name = listName
        )

        dao.insertList(list)
        val queriedList = dao.getList(list.id).first()

        assert(queriedList?.id == list.id)
        assert(queriedList?.name == listName)
        assert(queriedList?.ownerId == 1)
    }

    @Test
    @Throws(Exception::class)
    fun testRemovingList() = runTest {
        val listName = "Horror catalogue"
        val list = LibraryList(
            id = 1,
            ownerId = 1,
            name = listName
        )

        dao.insertList(list)
        dao.deleteListById(list.id)
        val queriedList = dao.getList(list.id).first()

        assert(queriedList == null)
    }

    @Test
    @Throws(Exception::class)
    fun testGettingLibraryListsByOwnerId() = runTest {
        val listName = "Catalog"
        val listsSize = 50
        repeat(listsSize) {
            dao.insertList(
                LibraryList(
                    id = 0,
                    ownerId = 1,
                    name = "$listName #$it"
                )
            )
        }

        val list = dao.getLists(1).first()
        assert(list.size >= listsSize)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateItemAndRead() = runTest {
        val initialName = "Catalogue #1"
        dao.insertList(
            LibraryList(
                id = 1,
                ownerId = 1,
                name = initialName
            )
        )

        var list = dao.getList(1).first()
        assert(list?.name == initialName)

        val finalName = "Trending"
        dao.updateList(list!!.copy(name = finalName))

        list = dao.getList(1).first()
        assert(list?.name == finalName)
    }
}
