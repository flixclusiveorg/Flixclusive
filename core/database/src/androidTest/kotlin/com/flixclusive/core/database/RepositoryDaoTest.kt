package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.provider.RepositoryDao
import com.flixclusive.core.database.entity.provider.InstalledRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RepositoryDaoTest {
    private lateinit var dao: RepositoryDao
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).build()
        dao = db.repositoryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun shouldInsertAndRetrieve() =
        runTest {
            val repo = InstalledRepository(
                url = "https://example.com/repo",
                owner = "owner1",
                name = "repo1",
                rawLinkFormat = "https://raw.example.com/%s",
            )

            dao.insert(repo)

            val result = dao.get(repo.url)
            expectThat(result).isNotNull().and {
                get { url }.isEqualTo(repo.url)
                get { owner }.isEqualTo("owner1")
                get { name }.isEqualTo("repo1")
            }
        }

    @Test
    fun shouldReturnNullForNonexistent() =
        runTest {
            expectThat(dao.get("https://nonexistent.com")).isNull()
        }

    @Test
    fun shouldGetAllAsFlow() =
        runTest {
            dao.insert(InstalledRepository(url = "https://a.com", owner = "a", name = "a", rawLinkFormat = "f"))
            dao.insert(InstalledRepository(url = "https://b.com", owner = "b", name = "b", rawLinkFormat = "f"))

            val all = dao.getAllAsFlow().first()
            expectThat(all).hasSize(2)
        }

    @Test
    fun shouldReturnEmptyFlowWhenEmpty() =
        runTest {
            val all = dao.getAllAsFlow().first()
            expectThat(all).isEmpty()
        }

    @Test
    fun shouldUpdate() =
        runTest {
            val repo = InstalledRepository(
                url = "https://example.com/repo",
                owner = "owner1",
                name = "repo1",
                rawLinkFormat = "https://raw.example.com/%s",
            )
            dao.insert(repo)

            dao.update(repo.copy(name = "updated-repo"))

            val result = dao.get(repo.url)
            expectThat(result).isNotNull().and {
                get { name }.isEqualTo("updated-repo")
            }
        }

    @Test
    fun shouldReplaceOnConflict() =
        runTest {
            val repo = InstalledRepository(
                url = "https://example.com/repo",
                owner = "owner1",
                name = "repo1",
                rawLinkFormat = "fmt",
            )
            dao.insert(repo)
            dao.insert(repo.copy(name = "replaced"))

            val result = dao.get(repo.url)
            expectThat(result).isNotNull().and {
                get { name }.isEqualTo("replaced")
            }
        }

    @Test
    fun shouldDelete() =
        runTest {
            val repo = InstalledRepository(
                url = "https://example.com/repo",
                owner = "owner1",
                name = "repo1",
                rawLinkFormat = "fmt",
            )
            dao.insert(repo)
            dao.delete(repo.url)

            expectThat(dao.get(repo.url)).isNull()
        }

    @Test
    fun shouldDeleteAll() =
        runTest {
            dao.insert(InstalledRepository(url = "https://a.com", owner = "a", name = "a", rawLinkFormat = "f"))
            dao.insert(InstalledRepository(url = "https://b.com", owner = "b", name = "b", rawLinkFormat = "f"))

            dao.deleteAll()

            val all = dao.getAllAsFlow().first()
            expectThat(all).isEmpty()
        }
}
