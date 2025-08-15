package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.LibraryListItemDao
import com.flixclusive.core.database.entity.DBFilm
import com.flixclusive.core.database.entity.LibraryList
import com.flixclusive.core.database.entity.LibraryListItem
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class LibraryListItemDaoTest {
    private lateinit var dao: LibraryListItemDao
    private lateinit var db: AppDatabase

    private val defaultFilm = DBFilm(
        id = "DB_TEST_FILM_ID",
        providerId = DEFAULT_FILM_SOURCE_NAME,
        title = "Database test film"
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context = context,
            klass = AppDatabase::class.java
        ).build()
        dao = db.libraryListItemDao()

        runBlocking {
            db.libraryListDao().insertList(
                LibraryList(
                    id = 0,
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
    fun testInsertItem() = runTest {
        val item = LibraryListItem(
            id = defaultFilm.identifier,
            film = defaultFilm
        )

        dao.insertItem(item)
        val queriedItem = dao.getItem(item.id).first()

        assert(queriedItem?.id == item.id)
        assert(queriedItem?.film?.providerId == DEFAULT_FILM_SOURCE_NAME)
    }

    @Test
    @Throws(Exception::class)
    fun testRemovingList() = runTest {
        val item = LibraryListItem(
            id = defaultFilm.identifier,
            film = defaultFilm
        )

        dao.insertItem(item)
        dao.deleteItemById(item.id)

        val queriedItem = dao.getItem(item.id).first()
        assert(queriedItem == null)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateItemAndRead() = runTest {
        var item: LibraryListItem? = LibraryListItem(
            id = defaultFilm.identifier,
            film = defaultFilm
        )

        dao.insertItem(item!!)

        item = dao.getItem(defaultFilm.identifier).first()
        assertNotNull(item)

        val finalName = "NEW_ID"
        val film = item!!.copy(film = item.film.copy(id = finalName))
        dao.updateItem(film)

        item = dao.getItem(defaultFilm.identifier).first()
        assertNotNull(item)
        assertEquals(item!!.film.identifier, finalName)
    }
}
