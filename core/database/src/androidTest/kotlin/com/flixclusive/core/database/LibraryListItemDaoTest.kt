package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.library.LibraryListItemDao
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
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
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
        filmId = defaultFilm.id,
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
    fun testInsertItem() =
        runTest {
            val itemId = dao.insert(defaultItem, defaultFilm)
            val queriedItem = dao.getAsFlow(itemId).first()

            expectThat(queriedItem).isNotNull().and {
                get { this.itemId }.isEqualTo(itemId)
                get { metadata.providerId }.isEqualTo(DEFAULT_FILM_SOURCE_NAME)
            }
        }

    @Test
    fun testRemoveItem() =
        runTest {
            val itemId = dao.insert(defaultItem, defaultFilm)
            dao.delete(itemId)

            val queriedItem = dao.getAsFlow(itemId).first()
            expectThat(queriedItem).isNull()
        }

    @Test
    fun testGetByListId() =
        runTest {
            val film2 = defaultFilm.copy(id = "SECOND_FILM_ID", title = "Second Film")
            dao.insert(defaultItem, defaultFilm)
            dao.insert(
                LibraryListItem(filmId = film2.id, listId = defaultList.id),
                film2,
            )

            val items = dao.getByListId(defaultList.id).first()
            expectThat(items).hasSize(2)
        }

    @Test
    fun testDeleteByListIdAndFilmId() =
        runTest {
            dao.insert(defaultItem, defaultFilm)
            dao.deleteByListIdAndFilmId(defaultList.id, defaultFilm.id)

            val items = dao.getByListId(defaultList.id).first()
            expectThat(items).hasSize(0)
        }

    @Test
    fun testCascadeDeleteOnListRemoval() =
        runTest {
            val itemId = dao.insert(defaultItem, defaultFilm)
            db.libraryListDao().deleteInternal(defaultList.id)

            val queriedItem = dao.getAsFlow(itemId).first()
            expectThat(queriedItem).isNull()
        }
}
