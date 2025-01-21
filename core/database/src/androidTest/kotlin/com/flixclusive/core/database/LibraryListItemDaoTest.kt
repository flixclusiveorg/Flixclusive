package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.LibraryListItemDao
import com.flixclusive.model.database.DBFilm
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
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
        dao.deleteItem(item)

        val queriedItem = dao.getItem(item.id).first()
        assert(queriedItem == null)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateItemAndRead() = runTest {
        val item = LibraryListItem(
            id = defaultFilm.identifier,
            film = defaultFilm
        )

        dao.insertItem(item)

        var list = dao.getItem(defaultFilm.identifier).first()
        assert(list != null)

        val finalName = "NEW_ID"
        dao.updateItem(list!!.copy(id = finalName))

        list = dao.getItem(finalName).first()
        assert(list != null)
    }
}
