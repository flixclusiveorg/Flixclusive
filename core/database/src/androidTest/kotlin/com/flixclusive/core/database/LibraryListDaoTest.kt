package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.flixclusive.core.database.dao.LibraryListDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.user.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class LibraryListDaoTest {
    private lateinit var dao: LibraryListDao
    private lateinit var db: AppDatabase

    private val defaultUser = User(
        id = 1,
        name = "test_user",
        image = 1,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).build()
        dao = db.libraryListDao()

        runBlocking {
            db.userDao().insert(defaultUser)
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testCreateList() =
        runTest {
            val listName = "Horror catalogue"
            val list = LibraryList(
                id = 1,
                ownerId = defaultUser.id,
                name = listName,
            )

            dao.insert(list)
            val queriedList = dao.get(list.id).first()

            assert(queriedList?.id == list.id)
            assert(queriedList?.name == listName)
            assert(queriedList?.ownerId == 1)
        }

    @Test
    @Throws(Exception::class)
    fun testRemovingList() =
        runTest {
            val listName = "Horror catalogue"
            val list = LibraryList(
                id = 1,
                ownerId = defaultUser.id,
                name = listName,
            )

            dao.insert(list)
            dao.delete(list.id)
            val queriedList = dao.get(list.id).first()

            assert(queriedList == null)
        }

    @Test
    @Throws(Exception::class)
    fun testGettingLibraryListsByOwnerId() =
        runTest {
            val listName = "Catalog"
            val listsSize = 50
            repeat(listsSize) {
                dao.insert(
                    LibraryList(
                        id = 0,
                        ownerId = defaultUser.id,
                        name = "$listName #$it",
                    ),
                )
            }

            val list = dao.getAll(1).first()
            assert(list.size >= listsSize)
        }

    @Test
    @Throws(Exception::class)
    fun testUpdateItemAndRead() =
        runTest {
            val initialName = "Catalogue #1"
            dao.insert(
                LibraryList(
                    id = 1,
                    ownerId = defaultUser.id,
                    name = initialName,
                ),
            )

            var list = dao.get(1).first()
            assert(list?.name == initialName)

            val finalName = "Trending"
            dao.update(list!!.copy(name = finalName))

            list = dao.get(1).first()
            assert(list?.name == finalName)
        }

    @Test
    @Throws(Exception::class)
    fun testGetListWithItems() =
        runTest {
            val list = LibraryList(
                id = 1,
                ownerId = defaultUser.id,
                name = "Test List",
            )

            dao.insert(list)

            val queriedListWithItems = dao.getListWithItems(list.id)

            queriedListWithItems.test {
                val item = awaitItem()

                expectThat(item).isNotNull().and {
                    get { list.id }.isEqualTo(list.id)
                    get { list.name }.isEqualTo(list.name)
                    get { list.ownerId }.isEqualTo(list.ownerId)
                    get { items }.isEmpty()
                }
            }
        }

    @Test
    @Throws(Exception::class)
    fun testGetUserWithListsAndItems() =
        runTest {
            val list = LibraryList(
                id = 1,
                ownerId = defaultUser.id,
                name = "User List",
            )

            dao.insert(list)

            val userWithListsAndItems = dao.getUserWithListsAndItems(defaultUser.id)

            userWithListsAndItems.test {
                val item = awaitItem()

                expectThat(item) {
                    get { user.id }.isEqualTo(defaultUser.id)
                    get { lists }.isNotEmpty()
                    get { lists.first().list.id }.isEqualTo(list.id)
                    get { lists.first().items }.isEmpty()
                }
            }
        }

    @Test
    @Throws(Exception::class)
    fun testGetAllListsContainingFilm() =
        runTest {
            val film = DBFilm(
                id = "filmId",
                providerId = "providerId",
                title = "Test Film",
            )

            repeat(2) {
                val list = LibraryList(
                    ownerId = defaultUser.id,
                    name = "List 1",
                )

                val listId = dao.insert(list)

                db.libraryListItemDao().insert(
                    item = LibraryListItem(
                        filmId = film.id,
                        listId = listId.toInt(),
                    ),
                    film = film
                )
            }

            dao.getListsContainingFilm(
                filmId = film.identifier,
                ownerId = defaultUser.id
            ).test {
                expectThat(awaitItem()).isNotEmpty().and {
                    get { size }.isEqualTo(2)
                }
            }
        }
}
