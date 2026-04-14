package com.flixclusive.data.backup.work

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.BackupOptions
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.data.backup.model.Backup
import com.flixclusive.data.backup.model.BackupDbFilm
import com.flixclusive.data.backup.model.BackupLibraryList
import com.flixclusive.data.backup.model.BackupLibraryListItem
import com.flixclusive.data.backup.repository.BackupResult
import com.flixclusive.model.film.util.FilmType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@OptIn(ExperimentalSerializationApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BackupRestoreWorkerTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var backupWorkManager: BackupWorkManager

    @Inject
    lateinit var userSessionDataStore: UserSessionDataStore

    @Inject
    lateinit var database: AppDatabase

    private lateinit var context: Context

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        hiltRule.inject()
    }

    @Test
    fun shouldRestoreLibraryListAndWriteResult() =
        runTest(testDispatcher) {
            val workManager = WorkManager.getInstance(context)

            // Ensure clean slate for this test run.
            withContext(Dispatchers.IO) {
                database.clearAllTables()
                userSessionDataStore.clearCurrentUser()
                workManager.cancelAllWork().result.get()
                workManager.pruneWork().result.get()
            }

            val userId = DatabaseTestDefaults.TEST_USER_ID
            database.userDao().insert(DatabaseTestDefaults.getUser(id = userId))
            userSessionDataStore.saveCurrentUserId(userId, 1)

            val listName = "Test List"
            val filmId = "film-1"
            val backupFile = createLibraryOnlyBackupFile(
                context = context,
                listName = listName,
                filmId = filmId,
            )

            try {
                val uniqueName = backupWorkManager.enqueueRestore(
                    userId = userId,
                    uri = Uri.fromFile(backupFile),
                    options = libraryOnlyOptions(),
                )

                val workInfo = awaitUniqueWorkFinished(
                    workManager = workManager,
                    uniqueWorkName = uniqueName,
                )

                expectThat(workInfo.state).isEqualTo(WorkInfo.State.SUCCEEDED)

                val restoreResult = backupWorkManager.readLastRestoreResult(userId)
                assertEmpty(restoreResult)

                val restoredLists = database.libraryListDao().getAll(userId)
                expectThat(restoredLists).hasSize(1)
                expectThat(restoredLists.first().name).isEqualTo(listName)
                expectThat(restoredLists.first().list.listType).isEqualTo(LibraryListType.CUSTOM)
                expectThat(restoredLists.first().items).hasSize(1)
                expectThat(restoredLists.first().items.first().filmId).isEqualTo(filmId)
            } finally {
                backupFile.delete()
            }
        }

    private suspend fun awaitUniqueWorkFinished(
        workManager: WorkManager,
        uniqueWorkName: String,
    ): WorkInfo {
        return withContext(Dispatchers.Default) {
            withTimeout(10_000) {
                while (true) {
                    val workInfo = workManager
                        .getWorkInfosForUniqueWork(uniqueWorkName)
                        .get()
                        .lastOrNull()

                    if (workInfo != null && workInfo.state.isFinished) {
                        return@withTimeout workInfo
                    }

                    delay(50)
                }

                error("Timeout waiting for work '$uniqueWorkName' to finish")
            }
        }
    }

    private fun createLibraryOnlyBackupFile(
        context: Context,
        listName: String,
        filmId: String,
    ): File {
        val timestamp = 1_700_000_000_000L

        val backup = Backup(
            libraryLists = listOf(
                BackupLibraryList(
                    name = listName,
                    description = null,
                    listType = LibraryListType.CUSTOM,
                    items = listOf(
                        BackupLibraryListItem(
                            listId = 0,
                            film = BackupDbFilm(
                                id = filmId,
                                title = "Test Film",
                                providerId = "test-provider",
                                adult = false,
                                filmType = FilmType.MOVIE,
                                overview = null,
                                posterImage = null,
                                language = null,
                                rating = null,
                                backdropImage = null,
                                releaseDate = null,
                                year = null,
                                createdAt = timestamp,
                                updatedAt = timestamp,
                            ),
                            createdAt = timestamp,
                            updatedAt = timestamp,
                        )
                    ),
                    createdAt = timestamp,
                    updatedAt = timestamp,
                )
            ),
            preferences = emptyList(),
            watchProgressList = emptyList(),
            searchHistory = emptyList(),
            providers = emptyList(),
            repositories = emptyList(),
        )

        val bytes = ProtoBuf.encodeToByteArray(Backup.serializer(), backup)

        return File(context.cacheDir, "backup_${System.currentTimeMillis()}.flxbackup")
            .apply {
                parentFile?.mkdirs()
                createNewFile()

                ZipOutputStream(outputStream().buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry("backup.pb"))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
    }

    private fun libraryOnlyOptions(): BackupOptions {
        return BackupOptions(
            includeLibrary = true,
            includeWatchProgress = false,
            includeSearchHistory = false,
            includePreferences = false,
            includeProviders = false,
            includeRepositories = false,
        )
    }

    private fun assertEmpty(result: BackupResult) {
        expectThat(result) {
            get { missingLibraryLists }.isEmpty()
            get { missingProviders }.isEmpty()
            get { missingProviderRepositories }.isEmpty()
            get { missingPreferences }.isEmpty()
            get { missingSearchHistory }.isEmpty()
            get { missingWatchProgress }.isEmpty()
        }
    }
}
