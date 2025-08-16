package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.LibraryListItemDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.user.User
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
        title = "Database test film",
    )

    private val defaultList = LibraryList(
        id = 1,
        ownerId = 1,
        name = "Default list",
    )

    private val defaultUser = User(
        id = 1,
        name = "test_user",
        image = 1,
    )

    private val defaultItem = LibraryListItem(
        filmId = defaultFilm.identifier,
        listId = defaultList.id,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).build()
        dao = db.libraryListItemDao()

        runBlocking {
            db.userDao().insert(defaultUser)
            db.libraryListDao().insert(defaultList)
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testInsertItem() =
        runTest {
            val itemId = dao.insert(defaultItem, defaultFilm)
            val queriedItem = dao.getAsFlow(itemId).first()

            assert(queriedItem?.itemId == itemId)
            assert(queriedItem?.metadata?.providerId == DEFAULT_FILM_SOURCE_NAME)
        }

    @Test
    @Throws(Exception::class)
    fun testRemovingList() =
        runTest {
            dao.insert(defaultItem, defaultFilm)
            dao.delete(defaultItem.id)

            val queriedItem = dao.getAsFlow(defaultItem.id).first()
            assert(queriedItem == null)
        }
}
