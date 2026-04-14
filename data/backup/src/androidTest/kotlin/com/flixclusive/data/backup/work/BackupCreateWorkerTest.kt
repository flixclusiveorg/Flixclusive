package com.flixclusive.data.backup.work

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.flixclusive.core.common.file.FileConstants
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.BackupOptions
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.data.backup.repository.BackupResult
import com.flixclusive.data.backup.work.util.BackupWorkConstants
import com.flixclusive.model.film.util.FilmType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import java.io.File
import java.util.Date
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BackupCreateWorkerTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var backupWorkManager: BackupWorkManager

    @Inject
    lateinit var userSessionDataStore: UserSessionDataStore

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    @Inject
    lateinit var database: AppDatabase

    private lateinit var context: Context

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        // Production app requests `MANAGE_EXTERNAL_STORAGE` and writes backups into public storage.
        // The androidTest APK doesn't have that access by default, so enable it via appops.
        allowAllFilesAccessForTestPackage()

        hiltRule.inject()
    }

    @Test
    fun shouldCreateBackupAndWriteResult() =
        runTest(testDispatcher) {
            val workManager = WorkManager.getInstance(context)

            val backupRoot = File(context.cacheDir, "backup-work-test")

            withContext(Dispatchers.IO) {
                database.clearAllTables()
                userSessionDataStore.clearCurrentUser()

                workManager.cancelAllWork().result.get()
                workManager.pruneWork().result.get()

                backupRoot.deleteRecursively()
                backupRoot.mkdirs()

                File(context.filesDir, "backup-work").deleteRecursively()
            }

            val userId = DatabaseTestDefaults.TEST_USER_ID
            database.userDao().insert(DatabaseTestDefaults.getUser(id = userId))
            userSessionDataStore.saveCurrentUserId(userId, 1)

            dataStoreManager.usePreferencesByUserId(userId)
            dataStoreManager.updateUserPrefs(UserPreferences.DATA_PREFS_KEY, DataPreferences::class) { old ->
                old.copy(
                    maxBackups = 1,
                    autoBackupOptions = libraryOnlyOptions(),
                )
            }

            dataStoreManager.updateSystemPrefs { old ->
                old.copy(storageDirectoryUri = Uri.fromFile(backupRoot).toString())
            }

            seedCustomLibraryList(
                db = database,
                ownerId = userId,
                listName = "Test List",
                filmId = "film-1",
            )

            val uniqueName = backupWorkManager.enqueueCreate(userId)

            val workInfo = awaitUniqueWorkFinished(
                workManager = workManager,
                uniqueWorkName = uniqueName,
            )

            expectThat(workInfo.state).isEqualTo(WorkInfo.State.SUCCEEDED)

            val result = backupWorkManager.readLastCreateResult(userId)
            assertEmpty(result)

            val userBackupDir = File(backupRoot, "backups/user-$userId")
            val backups = userBackupDir
                .listFiles()
                ?.filter { it.isFile && it.extension == FileConstants.BACKUP_FILE_EXTENSION }
                .orEmpty()

            expectThat(backups).hasSize(1)

            val resultFile = File(
                File(context.filesDir, "backup-work/user-$userId"),
                BackupWorkConstants.LAST_CREATE_RESULT_FILE_NAME,
            )
            expectThat(resultFile.exists()).isEqualTo(true)
        }

    @Test
    fun shouldRunPeriodicAutoBackupCreate() =
        runTest(testDispatcher) {
            val workManager = WorkManager.getInstance(context)
            val testDriver = requireNotNull(WorkManagerTestInitHelper.getTestDriver(context)) {
                "WorkManager test driver is null. Did WorkManagerTestInitHelper.initializeTestWorkManager(context) run?"
            }

            val backupRoot = File(context.cacheDir, "backup-work-test")

            withContext(Dispatchers.IO) {
                database.clearAllTables()
                userSessionDataStore.clearCurrentUser()

                workManager.cancelAllWork().result.get()
                workManager.pruneWork().result.get()

                backupRoot.deleteRecursively()
                backupRoot.mkdirs()

                File(context.filesDir, "backup-work").deleteRecursively()
            }

            val userId = DatabaseTestDefaults.TEST_USER_ID
            database.userDao().insert(DatabaseTestDefaults.getUser(id = userId))
            userSessionDataStore.saveCurrentUserId(userId, 1)

            dataStoreManager.usePreferencesByUserId(userId)
            dataStoreManager.updateUserPrefs(UserPreferences.DATA_PREFS_KEY, DataPreferences::class) { old ->
                old.copy(
                    maxBackups = 1,
                    autoBackupOptions = libraryOnlyOptions(),
                )
            }

            dataStoreManager.updateSystemPrefs { old ->
                old.copy(storageDirectoryUri = Uri.fromFile(backupRoot).toString())
            }

            seedCustomLibraryList(
                db = database,
                ownerId = userId,
                listName = "Test List",
                filmId = "film-1",
            )

            backupWorkManager.syncPeriodicAutoBackup(
                userId = userId,
                frequencyDays = 1,
            )

            val uniqueName = BackupWorkConstants.UNIQUE_AUTO_BACKUP_CREATE_PREFIX + userId
            val workInfo = awaitUniqueWorkEnqueued(
                workManager = workManager,
                uniqueWorkName = uniqueName,
            )

            // Periodic work doesn't run immediately in the test scheduler.
            testDriver.setPeriodDelayMet(workInfo.id)

            awaitBackupOutputWritten(userId = userId, backupRoot = backupRoot)

            val result = backupWorkManager.readLastCreateResult(userId)
            assertEmpty(result)

            val userBackupDir = File(backupRoot, "backups/user-$userId")
            val backups = userBackupDir
                .listFiles()
                ?.filter { it.isFile && it.extension == FileConstants.BACKUP_FILE_EXTENSION }
                .orEmpty()

            expectThat(backups).hasSize(1)

            val resultFile = File(
                File(context.filesDir, "backup-work/user-$userId"),
                BackupWorkConstants.LAST_CREATE_RESULT_FILE_NAME,
            )
            expectThat(resultFile.exists()).isEqualTo(true)
        }

    private suspend fun awaitUniqueWorkFinished(
        workManager: WorkManager,
        uniqueWorkName: String,
    ): WorkInfo {
        return withContext(Dispatchers.Default) {
            withTimeout(15_000) {
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

    private suspend fun awaitUniqueWorkEnqueued(
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

                    if (workInfo != null) {
                        return@withTimeout workInfo
                    }

                    delay(50)
                }

                error("Timeout waiting for work '$uniqueWorkName' to be enqueued")
            }
        }
    }

    private suspend fun awaitBackupOutputWritten(userId: String, backupRoot: File) {
        val userBackupDir = File(backupRoot, "backups/user-$userId")

        withContext(Dispatchers.Default) {
            withTimeout(15_000) {
                while (true) {
                    val backups = userBackupDir
                        .listFiles()
                        ?.filter { it.isFile && it.extension == FileConstants.BACKUP_FILE_EXTENSION }
                        .orEmpty()

                    val hasResultFile = File(
                        File(context.filesDir, "backup-work/user-$userId"),
                        BackupWorkConstants.LAST_CREATE_RESULT_FILE_NAME,
                    ).exists()

                    if (backups.isNotEmpty() && hasResultFile) {
                        return@withTimeout
                    }

                    delay(50)
                }

                error("Timeout waiting for backup output for userId=$userId")
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

    private suspend fun seedCustomLibraryList(
        db: AppDatabase,
        ownerId: String,
        listName: String,
        filmId: String,
    ) {
        val fixedDate = Date(1_700_000_000_000)

        val listId = db.libraryListDao().insert(
            LibraryList(
                ownerId = ownerId,
                name = listName,
                description = null,
                listType = LibraryListType.CUSTOM,
                createdAt = fixedDate,
                updatedAt = fixedDate,
            )
        ).toInt()

        db.libraryListItemDao().upsertFilm(
            DBFilm(
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
                createdAt = fixedDate,
                updatedAt = fixedDate,
            )
        )

        db.libraryListItemDao().insertItem(
            LibraryListItem(
                filmId = filmId,
                listId = listId,
                createdAt = fixedDate,
                updatedAt = fixedDate,
            )
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

    private fun allowAllFilesAccessForTestPackage() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = context.packageName

        runCatching {
            instrumentation.uiAutomation
                .executeShellCommand("cmd appops set $packageName MANAGE_EXTERNAL_STORAGE allow")
                .close()
        }

        runCatching {
            instrumentation.uiAutomation
                .executeShellCommand("appops set $packageName MANAGE_EXTERNAL_STORAGE allow")
                .close()
        }
    }
}
