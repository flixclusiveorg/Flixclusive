package com.flixclusive.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.LibraryListCrossRefDao
import com.flixclusive.core.database.dao.LibraryListDao
import com.flixclusive.core.database.dao.LibraryListItemDao
import com.flixclusive.model.database.DBFilm
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListAndItemCrossRef
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.database.User
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class LibraryListCrossRefDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var crossRefDao: LibraryListCrossRefDao
    private lateinit var listDao: LibraryListDao
    private lateinit var itemDao: LibraryListItemDao

    private val defaultUser =
        User(
            id = 1,
            image = 0,
            name = "Test",
        )
    private val defaultFilm =
        DBFilm(
            id = "DB_TEST_FILM_ID",
            providerId = DEFAULT_FILM_SOURCE_NAME,
            title = "Database test film",
        )

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AppDatabase::class.java,
                ).allowMainThreadQueries()
                .build()

        runBlocking {
            database.userDao().insert(defaultUser)
        }

        crossRefDao = database.libraryListCrossRefDao()
        listDao = database.libraryListDao()
        itemDao = database.libraryListItemDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndDeleteCrossRef() =
        runTest {
            val list =
                LibraryList(
                    id = 1,
                    ownerId = defaultUser.id,
                    name = "My List",
                    createdAt = Date(),
                    updatedAt = Date(),
                )

            val film =
                defaultFilm.copy(
                    id = "film1",
                    title = "Test Film",
                )

            val item =
                LibraryListItem(
                    id = film.identifier,
                    film = film,
                )

            listDao.insertList(list)
            itemDao.insertItem(item)
            val crossRef =
                LibraryListAndItemCrossRef(
                    listId = list.id,
                    itemId = film.identifier,
                )
            crossRefDao.insertCrossRef(crossRef)

            val userWithLists = crossRefDao.getUserWithListsAndItems(defaultUser.id).first()
            assertNotNull(userWithLists)
            assertTrue(userWithLists!!.list.isNotEmpty())
            assertTrue(
                userWithLists.list
                    .first()
                    .items
                    .isNotEmpty(),
            )

            crossRefDao.deleteCrossRef(crossRef)

            val updatedUserWithLists = crossRefDao.getUserWithListsAndItems(defaultUser.id).first()
            assertNotNull(updatedUserWithLists)
            assertTrue(
                updatedUserWithLists!!
                    .list
                    .first()
                    .items
                    .isEmpty(),
            )
        }

    @Test
    fun deleteCrossRefById() =
        runTest {
            val list =
                LibraryList(
                    id = 1,
                    ownerId = defaultUser.id,
                    name = "My List",
                    createdAt = Date(),
                    updatedAt = Date(),
                )

            val film =
                DBFilm(
                    id = "film1",
                    title = "Test Film",
                )

            val item =
                LibraryListItem(
                    id = film.identifier,
                    film = film,
                )

            listDao.insertList(list)
            itemDao.insertItem(item)

            val crossRef =
                LibraryListAndItemCrossRef(
                    listId = list.id,
                    itemId = film.identifier,
                )
            crossRefDao.insertCrossRef(crossRef)

            crossRefDao.deleteCrossRefById(list.id, film.identifier)

            val userWithLists = crossRefDao.getUserWithListsAndItems(defaultUser.id).first()
            assertNotNull(userWithLists)
            assertTrue(userWithLists!!.list.isNotEmpty())
            assertTrue(
                userWithLists.list
                    .first()
                    .items
                    .isEmpty(),
            )
        }

    @Test
    fun getUserWithListsAndItems() =
        runTest {
            val lists =
                listOf(
                    LibraryList(
                        id = 1,
                        ownerId = defaultUser.id,
                        name = "List 1",
                        createdAt = Date(),
                        updatedAt = Date(),
                    ),
                    LibraryList(
                        id = 2,
                        ownerId = defaultUser.id,
                        name = "List 2",
                        createdAt = Date(),
                        updatedAt = Date(),
                    ),
                )

            val films =
                listOf(
                    defaultFilm.copy(
                        id = "film1",
                        title = "Film 1",
                    ),
                    defaultFilm.copy(
                        id = "film2",
                        title = "Film 2",
                    ),
                )

            val items =
                films.map { film ->
                    LibraryListItem(
                        id = film.identifier,
                        film = film,
                    )
                }

            // Insert all data
            lists.forEach { listDao.insertList(it) }
            items.forEach {
                itemDao.insertItem(it)
                crossRefDao.insertCrossRef(
                    LibraryListAndItemCrossRef(
                        listId = 2,
                        itemId = it.id,
                    ),
                )
            }

            val userWithLists = crossRefDao.getUserWithListsAndItems(defaultUser.id).first()
            assertNotNull(userWithLists)
            assertEquals(2, userWithLists!!.list.size)
            assertEquals(2, userWithLists.list[1].items.size)
            assertEquals(0, userWithLists.list[0].items.size)
        }
}
