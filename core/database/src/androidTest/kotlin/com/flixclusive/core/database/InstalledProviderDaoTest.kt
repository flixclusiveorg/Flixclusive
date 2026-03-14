package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.provider.InstalledProviderDao
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.database.entity.provider.InstalledRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class InstalledProviderDaoTest {
    private lateinit var dao: InstalledProviderDao
    private lateinit var db: AppDatabase

    private val defaultRepo = InstalledRepository(
        url = "https://example.com/repo",
        owner = "owner",
        name = "repo",
        rawLinkFormat = "https://raw.example.com/%s",
    )

    private fun provider(
        id: String,
        sortOrder: Double = 1.0,
        isDisabled: Boolean = false,
        repositoryUrl: String = defaultRepo.url,
    ) = InstalledProvider(
        id = id,
        repositoryUrl = repositoryUrl,
        name = "Provider $id",
        status = "active",
        providerType = "movie",
        language = "en",
        adult = false,
        versionName = "1.0.0",
        versionCode = 1,
        buildUrl = "https://example.com/$id.flx",
        isDisabled = isDisabled,
        sortOrder = sortOrder,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).build()
        dao = db.installedProviderDao()

        runBlocking {
            db.repositoryDao().insert(defaultRepo)
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun shouldInsertAndRetrieve() =
        runTest {
            val p = provider("p1")
            dao.insert(p)

            val result = dao.get("p1")
            expectThat(result).isNotNull().and {
                get { id }.isEqualTo("p1")
                get { name }.isEqualTo("Provider p1")
                get { repositoryUrl }.isEqualTo(defaultRepo.url)
            }
        }

    @Test
    fun shouldReturnNullForNonexistent() =
        runTest {
            expectThat(dao.get("nonexistent")).isNull()
        }

    @Test
    fun shouldGetAllOrderedBySortOrder() =
        runTest {
            dao.insert(provider("p3", sortOrder = 3.0))
            dao.insert(provider("p1", sortOrder = 1.0))
            dao.insert(provider("p2", sortOrder = 2.0))

            val all = dao.getAllOrderedBySortOrder().first()
            expectThat(all).hasSize(3)
            expectThat(all.map { it.id }).isEqualTo(listOf("p1", "p2", "p3"))
        }

    @Test
    fun shouldGetEnabledOnly() =
        runTest {
            dao.insert(provider("p1", sortOrder = 1.0, isDisabled = false))
            dao.insert(provider("p2", sortOrder = 2.0, isDisabled = true))
            dao.insert(provider("p3", sortOrder = 3.0, isDisabled = false))

            val enabled = dao.getEnabled().first()
            expectThat(enabled).hasSize(2)
            expectThat(enabled.map { it.id }).isEqualTo(listOf("p1", "p3"))
        }

    @Test
    fun shouldGetByRepositoryUrl() =
        runTest {
            val repo2 = InstalledRepository(
                url = "https://other.com/repo",
                owner = "other",
                name = "other",
                rawLinkFormat = "fmt",
            )
            db.repositoryDao().insert(repo2)

            dao.insert(provider("p1", sortOrder = 1.0))
            dao.insert(provider("p2", sortOrder = 2.0, repositoryUrl = repo2.url))

            val result = dao.getByRepositoryUrl(defaultRepo.url).first()
            expectThat(result).hasSize(1)
            expectThat(result.first().id).isEqualTo("p1")
        }

    @Test
    fun shouldUpdateSortOrder() =
        runTest {
            dao.insert(provider("p1", sortOrder = 1.0))

            dao.updateSortOrder("p1", 5.0)

            val result = dao.get("p1")
            expectThat(result).isNotNull().and {
                get { sortOrder }.isEqualTo(5.0)
            }
        }

    @Test
    fun shouldSetDisabled() =
        runTest {
            dao.insert(provider("p1", isDisabled = false))

            dao.setDisabled("p1", true)

            val result = dao.get("p1")
            expectThat(result).isNotNull().and {
                get { isDisabled }.isTrue()
            }

            dao.setDisabled("p1", false)

            val result2 = dao.get("p1")
            expectThat(result2).isNotNull().and {
                get { isDisabled }.isFalse()
            }
        }

    @Test
    fun shouldGetMaxSortOrder() =
        runTest {
            dao.insert(provider("p1", sortOrder = 1.0))
            dao.insert(provider("p2", sortOrder = 5.5))
            dao.insert(provider("p3", sortOrder = 3.0))

            expectThat(dao.getMaxSortOrder()).isEqualTo(5.5)
        }

    @Test
    fun shouldReturnNullMaxSortOrderWhenEmpty() =
        runTest {
            expectThat(dao.getMaxSortOrder()).isNull()
        }

    @Test
    fun shouldDelete() =
        runTest {
            dao.insert(provider("p1"))
            dao.delete("p1")

            expectThat(dao.get("p1")).isNull()
        }

    @Test
    fun shouldDeleteAll() =
        runTest {
            dao.insert(provider("p1", sortOrder = 1.0))
            dao.insert(provider("p2", sortOrder = 2.0))

            dao.deleteAll()

            val all = dao.getAllOrderedBySortOrder().first()
            expectThat(all).isEmpty()
        }

    @Test
    fun shouldCascadeDeleteOnRepositoryRemoval() =
        runTest {
            dao.insert(provider("p1"))

            db.repositoryDao().delete(defaultRepo.url)

            expectThat(dao.get("p1")).isNull()
        }

    @Test
    fun shouldReturnEmptyFlowWhenNoProviders() =
        runTest {
            val all = dao.getAllOrderedBySortOrder().first()
            expectThat(all).isEmpty()
        }
}
