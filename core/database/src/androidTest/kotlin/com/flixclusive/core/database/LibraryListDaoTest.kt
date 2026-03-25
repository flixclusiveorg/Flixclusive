package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.database.entity.library.SystemListDeletionException
import com.flixclusive.core.database.entity.user.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isNull
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
    fun testCreateList() =
        runTest {
            val listName = "Horror catalogue"
            val list = LibraryList(
                id = 1,
                ownerId = defaultUser.id,
                name = listName,
            )

            dao.insert(list)
            val queriedList = dao.getAsFlow(list.id).first()

            expectThat(queriedList).isNotNull().and {
                get { id }.isEqualTo(list.id)
                get { name }.isEqualTo(listName)
                get { ownerId }.isEqualTo(1)
                get { listType }.isEqualTo(LibraryListType.CUSTOM)
            }
        }

    @Test
    fun testRemovingCustomList() =
        runTest {
            val list = LibraryList(
                id = 1,
                ownerId = defaultUser.id,
                name = "Horror catalogue",
            )

            dao.insert(list)
            dao.deleteSafe(list.id)
            val queriedList = dao.getAsFlow(list.id).first()

            expectThat(queriedList).isNull()
        }

    @Test
    fun testDeleteSafeBlocksSystemListDeletion() =
        runTest {
            dao.seedWatchedList(defaultUser.id)
            val watched = dao.getByType(defaultUser.id, LibraryListType.WATCHED).first()

            expectThrows<SystemListDeletionException> {
                dao.deleteSafe(watched.id)
            }

            // List should still exist
            expectThat(dao.getAsFlow(watched.id).first()).isNotNull()
        }

    @Test
    fun testGettingLibraryListsByOwnerId() =
        runTest {
            val listName = "Catalog"
            val listsSize = 50
            repeat(listsSize) {
                dao.insert(
                    LibraryList(
                        ownerId = defaultUser.id,
                        name = "$listName #$it",
                    ),
                )
            }

            val list = dao.getAllAsFlow(defaultUser.id).first()
            expectThat(list.size).isEqualTo(listsSize)
        }

    @Test
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

            var list = dao.getAsFlow(1).first()
            expectThat(list?.name).isEqualTo(initialName)

            val finalName = "Trending"
            dao.update(list!!.copy(name = finalName))

            list = dao.getAsFlow(1).first()
            expectThat(list?.name).isEqualTo(finalName)
        }

    @Test
    fun testGetAsFlowListWithItems() =
        runTest {
            val list = LibraryList(
                id = 1,
                ownerId = defaultUser.id,
                name = "Test List",
            )

            dao.insert(list)

            dao.getListWithItemsAsFlow(list.id).test {
                val item = awaitItem()

                expectThat(item).isNotNull().and {
                    get { this.list.id }.isEqualTo(list.id)
                    get { this.list.name }.isEqualTo(list.name)
                    get { this.list.ownerId }.isEqualTo(list.ownerId)
                    get { items }.isEmpty()
                }
            }
        }

    @Test
    fun testGetAsFlowUserWithListsAndItems() =
        runTest {
            val list = LibraryList(
                id = 1,
                ownerId = defaultUser.id,
                name = "User List",
            )

            dao.insert(list)

            dao.getUserWithListsAndItemsAsFlow(defaultUser.id).test {
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
    fun testGetAsFlowAllListsContainingFilm() =
        runTest {
            val film = DBFilm(
                id = "filmId",
                providerId = "providerId",
                title = "Test Film",
            )

            repeat(2) {
                val list = LibraryList(
                    ownerId = defaultUser.id,
                    name = "List $it",
                )

                val listId = dao.insert(list)

                db.libraryListItemDao().insert(
                    item = LibraryListItem(
                        filmId = film.id,
                        listId = listId.toInt(),
                    ),
                    film = film,
                )
            }

            dao.getListsContainingFilmAsFlow(
                filmId = film.id,
                ownerId = defaultUser.id,
            ).test {
                expectThat(awaitItem()).isNotEmpty().and {
                    get { size }.isEqualTo(2)
                }
            }
        }

    @Test
    fun testSeedWatchedList() =
        runTest {
            dao.seedWatchedList(defaultUser.id)

            val watchedList = dao.getByType(defaultUser.id, LibraryListType.WATCHED)
            expectThat(watchedList).hasSize(1)

            expectThat(watchedList.firstOrNull())
                .isNotNull()
                .and {
                    get { ownerId }.isEqualTo(defaultUser.id)
                    get { listType }.isEqualTo(LibraryListType.WATCHED)
                }
        }

    @Test
    fun testSeedWatchedListIsIdempotent() =
        runTest {
            dao.seedWatchedList(defaultUser.id)
            dao.seedWatchedList(defaultUser.id)

            val list = dao.getByType(defaultUser.id, LibraryListType.WATCHED)
            expectThat(list).hasSize(1)
        }

    @Test
    fun testGetAsFlowByType() =
        runTest {
            dao.seedWatchedList(defaultUser.id)
            dao.insert(LibraryList(ownerId = defaultUser.id, name = "Custom 1"))
            dao.insert(LibraryList(ownerId = defaultUser.id, name = "Custom 2"))

            val customLists = dao.getByType(defaultUser.id, LibraryListType.CUSTOM)
            expectThat(customLists).hasSize(2)

            val watched = dao.getByType(defaultUser.id, LibraryListType.WATCHED)
            expectThat(watched).hasSize(1)
        }

    @Test
    fun testDeleteNonExistentListIsNoOp() =
        runTest {
            // Should not throw
            dao.deleteSafe(999)
        }
}
