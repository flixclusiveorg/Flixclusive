package com.flixclusive.data.backup.repository.impl

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.ProviderConstants
import com.flixclusive.core.common.provider.ProviderFile.getProvidersPath
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.data.backup.create.impl.LibraryListBackupCreator
import com.flixclusive.data.backup.create.impl.PreferenceBackupCreator
import com.flixclusive.data.backup.create.impl.ProviderBackupCreator
import com.flixclusive.data.backup.create.impl.RepositoryBackupCreator
import com.flixclusive.data.backup.create.impl.SearchHistoryBackupCreator
import com.flixclusive.data.backup.create.impl.WatchProgressBackupCreator
import com.flixclusive.data.backup.model.Backup
import com.flixclusive.data.backup.model.BackupOptions
import com.flixclusive.data.backup.repository.BackupResult
import com.flixclusive.data.backup.restore.impl.LibraryListBackupRestorer
import com.flixclusive.data.backup.restore.impl.PreferenceBackupRestorer
import com.flixclusive.data.backup.restore.impl.ProviderBackupRestorer
import com.flixclusive.data.backup.restore.impl.RepositoryBackupRestorer
import com.flixclusive.data.backup.restore.impl.SearchHistoryBackupRestorer
import com.flixclusive.data.backup.restore.impl.WatchProgressBackupRestorer
import com.flixclusive.data.backup.validate.impl.LibraryListBackupValidator
import com.flixclusive.data.backup.validate.impl.PreferenceBackupValidator
import com.flixclusive.data.backup.validate.impl.ProviderBackupValidator
import com.flixclusive.data.backup.validate.impl.RepositoryBackupValidator
import com.flixclusive.data.backup.validate.impl.SearchHistoryBackupValidator
import com.flixclusive.data.backup.validate.impl.WatchProgressBackupValidator
import com.flixclusive.model.film.util.FilmType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.zip.ZipInputStream
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class)
@RunWith(AndroidJUnit4::class)
class BackupRepositoryImplTest {
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun shouldCreateBackupFile() =
        runTest(testDispatcher) {
            val db = DatabaseTestDefaults.createDatabase(context)
            val backupFile = createBackupFile(context)
            try {
                val userId = insertUser(db)
                val userSession = TestUserSessionDataStore(userId)
                val repository = createRepository(
                    context = context,
                    db = db,
                    userSessionDataStore = userSession,
                    appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
                )

                val listName = "Test List"
                val filmId = "film-1"
                seedCustomLibraryList(db, ownerId = userId, listName = listName, filmId = filmId)

                repository.create(uri = Uri.fromFile(backupFile), options = libraryOnlyOptions())

                val backup = decodeBackup(backupFile)

                expectThat(backup.libraryLists).hasSize(1)

                val backedUpList = backup.libraryLists.first()
                expectThat(backedUpList.name).isEqualTo(listName)
                expectThat(backedUpList.listType).isEqualTo(LibraryListType.CUSTOM)
                expectThat(backedUpList.items).hasSize(1)
                expectThat(backedUpList.items.first().film.id).isEqualTo(filmId)

                expectThat(backup.preferences).isEmpty()
                expectThat(backup.watchProgressList).isEmpty()
                expectThat(backup.searchHistory).isEmpty()
                expectThat(backup.providers).isEmpty()
                expectThat(backup.repositories).isEmpty()
            } finally {
                db.close()
                backupFile.delete()
            }
        }

    @Test
    fun shouldValidateCreatedBackup() =
        runTest(testDispatcher) {
            val db = DatabaseTestDefaults.createDatabase(context)
            val backupFile = createBackupFile(context)
            try {
                val userId = insertUser(db)
                val userSession = TestUserSessionDataStore(userId)
                val repository = createRepository(
                    context = context,
                    db = db,
                    userSessionDataStore = userSession,
                    appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
                )

                seedCustomLibraryList(db, ownerId = userId, listName = "Test List", filmId = "film-1")

                val result = repository.create(uri = Uri.fromFile(backupFile), options = libraryOnlyOptions())

                assertEmpty(result)
            } finally {
                db.close()
                backupFile.delete()
            }
        }

    @Test
    fun shouldIncludeAndRestoreProviderFiles() =
        runTest(testDispatcher) {
            val db = DatabaseTestDefaults.createDatabase(context)
            val backupFile = createBackupFile(context)
            var providersDir: File? = null
            try {
                val userId = insertUser(db)
                val userSession = TestUserSessionDataStore(userId)
                val repository = createRepository(
                    context = context,
                    db = db,
                    userSessionDataStore = userSession,
                    appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
                )

                providersDir = File(context.getProvidersPath(userId)).apply { mkdirs() }
                val repositoryDir = File(providersDir, "test-repo").apply { mkdirs() }
                val providerFile = File(repositoryDir, "BasicDummyProvider.flx").apply { writeText("dummy content") }
                File(repositoryDir, ProviderConstants.UPDATER_JSON_FILE).apply {
                    writeText("""
                        [{
                            "repositoryUrl": "https://github.com/flixclusiveorg/providers-template",
                            "adult": false,
                            "providerType": {
                                "type": "Movies, TV Shows, etc."
                            },
                            "status": "Working",
                            "language": {
                                "languageCode": "Multiple"
                            },
                            "authors": [
                                {
                                    "image": "http://github.com/flixclusiveorg.png",
                                    "name": "flixclusiveorg",
                                    "socialLink": "http://github.com/flixclusiveorg"
                                }
                            ],
                            "id": "14a5037ac9553dd",
                            "versionCode": 10000,
                            "description": "A dummy provider that does nothing.",
                            "buildUrl": "https://raw.githubusercontent.com/flixclusiveorg/providers-template/builds/BasicDummyProvider.flx",
                            "changelog": "# Header\n## Secondary header\n---\n\nList\n- Item 1\n- Item 2\n- Item 3",
                            "versionName": "1.0.0",
                            "name": "Test Provider"
                        }]
                    """.trimIndent())
                }

                val repositoryUrl = "https://example.com/repo"
                db.repositoryDao().insert(
                    DatabaseTestDefaults.getInstalledRepository(
                        url = repositoryUrl,
                        userId = userId,
                    )
                )
                db.installedProviderDao().insert(
                    DatabaseTestDefaults.getInstalledProvider(
                        id = "14a5037ac9553dd",
                        ownerId = userId,
                        repositoryUrl = repositoryUrl,
                        filePath = providerFile.absolutePath,
                    )
                )

                val createResult = repository.create(
                    uri = Uri.fromFile(backupFile),
                    options = BackupOptions(
                        includeLibrary = false,
                        includeWatchProgress = false,
                        includeSearchHistory = false,
                        includePreferences = false,
                        includeProviders = true,
                        includeRepositories = true,
                    ),
                )

                providerFile.writeText("old")
                val restoreResult = repository.restore(uri = Uri.fromFile(backupFile))

                assertEmpty(createResult)
                assertEmpty(restoreResult)
            } finally {
                db.close()
                providersDir?.deleteRecursively()
                backupFile.delete()
            }
        }

    @Test
    fun shouldRestoreBackup() =
        runTest(testDispatcher) {
            val backupFile = createLibraryOnlyBackupFile(context)
            val targetDb = DatabaseTestDefaults.createDatabase(context)
            try {
                val userId = insertUser(targetDb)
                val userSession = TestUserSessionDataStore(userId)
                val repository = createRepository(
                    context = context,
                    db = targetDb,
                    userSessionDataStore = userSession,
                    appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
                )

                repository.restore(uri = Uri.fromFile(backupFile))

                val restoredLists = targetDb.libraryListDao().getAll(userId)
                expectThat(restoredLists).hasSize(1)
                expectThat(restoredLists.first().name).isEqualTo("Test List")
                expectThat(restoredLists.first().list.listType).isEqualTo(LibraryListType.CUSTOM)
                expectThat(restoredLists.first().items).hasSize(1)
                expectThat(restoredLists.first().items.first().filmId).isEqualTo("film-1")
            } finally {
                targetDb.close()
                backupFile.delete()
            }
        }

    @Test
    fun shouldValidateRestoredBackup() =
        runTest(testDispatcher) {
            val backupFile = createLibraryOnlyBackupFile(context)
            val targetDb = DatabaseTestDefaults.createDatabase(context)
            try {
                val userId = insertUser(targetDb)
                val userSession = TestUserSessionDataStore(userId)
                val repository = createRepository(
                    context = context,
                    db = targetDb,
                    userSessionDataStore = userSession,
                    appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
                )

                val result = repository.restore(uri = Uri.fromFile(backupFile))

                assertEmpty(result)
            } finally {
                targetDb.close()
                backupFile.delete()
            }
        }

    private suspend fun insertUser(db: AppDatabase): Int {
        val rowId = db.userDao().insert(DatabaseTestDefaults.getUser(id = 0))
        return rowId.toInt()
    }

    private suspend fun seedCustomLibraryList(
        db: AppDatabase,
        ownerId: Int,
        listName: String,
        filmId: String,
    ) {
        val listId = db.libraryListDao().insert(
            LibraryList(
                ownerId = ownerId,
                name = listName,
                description = null,
                listType = LibraryListType.CUSTOM,
                createdAt = Date(1_700_000_000_000),
                updatedAt = Date(1_700_000_000_000),
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
                createdAt = Date(1_700_000_000_000),
                updatedAt = Date(1_700_000_000_000),
            )
        )

        db.libraryListItemDao().insertItem(
            LibraryListItem(
                filmId = filmId,
                listId = listId,
                createdAt = Date(1_700_000_000_000),
                updatedAt = Date(1_700_000_000_000),
            )
        )
    }

    private fun createBackupFile(context: Context): File {
        return File(context.cacheDir, "backup_${System.currentTimeMillis()}.flx").apply {
            parentFile?.mkdirs()
            createNewFile()
        }
    }

    private suspend fun createLibraryOnlyBackupFile(context: Context): File {
        val db = DatabaseTestDefaults.createDatabase(context)
        val backupFile = createBackupFile(context)
        try {
            val userId = insertUser(db)
            val userSession = TestUserSessionDataStore(userId)
            val repository = createRepository(
                context = context,
                db = db,
                userSessionDataStore = userSession,
                appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
            )

            seedCustomLibraryList(db, ownerId = userId, listName = "Test List", filmId = "film-1")

            repository.create(uri = Uri.fromFile(backupFile), options = libraryOnlyOptions())

            return backupFile
        } catch (t: Throwable) {
            backupFile.delete()
            throw t
        } finally {
            db.close()
        }
    }

    private fun decodeBackup(file: File): Backup {
        file.inputStream().use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && entry.name == "backup.pb") {
                        val bytes = zip.readBytes()
                        return ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
                    }
                    zip.closeEntry()
                }
            }
        }

        throw IOException("Backup archive is missing 'backup.pb'")
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

    private fun createRepository(
        context: Context,
        db: AppDatabase,
        userSessionDataStore: UserSessionDataStore,
        appDispatchers: AppDispatchers,
    ): BackupRepositoryImpl {
        val libraryListDao = db.libraryListDao()
        val libraryListItemDao = db.libraryListItemDao()
        val episodeProgressDao = db.episodeProgressDao()
        val movieProgressDao = db.movieProgressDao()
        val searchHistoryDao = db.searchHistoryDao()
        val installedProviderDao = db.installedProviderDao()
        val repositoryDao = db.repositoryDao()

        return BackupRepositoryImpl(
            context = context,
            appDispatchers = appDispatchers,
            userSessionDataStore = userSessionDataStore,
            libraryListBackupValidator = LibraryListBackupValidator(
                libraryListDao = libraryListDao,
                userSessionDataStore = userSessionDataStore,
            ),
            preferenceBackupValidator = PreferenceBackupValidator(
                context = context,
                userSessionDataStore = userSessionDataStore,
            ),
            watchProgressBackupValidator = WatchProgressBackupValidator(
                episodeProgressDao = episodeProgressDao,
                movieProgressDao = movieProgressDao,
                userSessionDataStore = userSessionDataStore,
            ),
            searchHistoryBackupValidator = SearchHistoryBackupValidator(
                searchHistoryDao = searchHistoryDao,
                userSessionDataStore = userSessionDataStore,
            ),
            providerBackupValidator = ProviderBackupValidator(
                installedProviderDao = installedProviderDao,
                userSessionDataStore = userSessionDataStore,
            ),
            repositoryBackupValidator = RepositoryBackupValidator(
                installedRepositoryDao = repositoryDao,
                userSessionDataStore = userSessionDataStore,
            ),
            libraryListBackupCreator = LibraryListBackupCreator(
                libraryListDao = libraryListDao,
                userSessionDataStore = userSessionDataStore,
            ),
            preferenceBackupCreator = PreferenceBackupCreator(
                context = context,
                userSessionDataStore = userSessionDataStore,
            ),
            watchProgressBackupCreator = WatchProgressBackupCreator(
                episodeProgressDao = episodeProgressDao,
                movieProgressDao = movieProgressDao,
                userSessionDataStore = userSessionDataStore,
            ),
            searchHistoryBackupCreator = SearchHistoryBackupCreator(
                searchHistoryDao = searchHistoryDao,
                userSessionDataStore = userSessionDataStore,
            ),
            providerBackupCreator = ProviderBackupCreator(
                installedProviderDao = installedProviderDao,
                userSessionDataStore = userSessionDataStore,
            ),
            repositoryBackupCreator = RepositoryBackupCreator(
                installedRepositoryDao = repositoryDao,
                userSessionDataStore = userSessionDataStore,
            ),
            libraryListBackupRestorer = LibraryListBackupRestorer(
                libraryListDao = libraryListDao,
                libraryListItemDao = libraryListItemDao,
                userSessionDataStore = userSessionDataStore,
            ),
            preferenceBackupRestorer = PreferenceBackupRestorer(
                dataStoreManager = NoOpDataStoreManager,
            ),
            watchProgressBackupRestorer = WatchProgressBackupRestorer(
                episodeProgressDao = episodeProgressDao,
                movieProgressDao = movieProgressDao,
                libraryListItemDao = libraryListItemDao,
                libraryListDao = libraryListDao,
                userSessionDataStore = userSessionDataStore,
            ),
            searchHistoryBackupRestorer = SearchHistoryBackupRestorer(
                searchHistoryDao = searchHistoryDao,
                userSessionDataStore = userSessionDataStore,
            ),
            providerBackupRestorer = ProviderBackupRestorer(
                context = context,
                installedProviderDao = installedProviderDao,
                userSessionDataStore = userSessionDataStore,
            ),
            repositoryBackupRestorer = RepositoryBackupRestorer(
                installedRepositoryDao = repositoryDao,
                userSessionDataStore = userSessionDataStore,
            ),
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

    private class TestUserSessionDataStore(initialUserId: Int) : UserSessionDataStore {
        private val currentUserIdState = MutableStateFlow<Int?>(initialUserId)
        private val sessionTimeoutState = MutableStateFlow(0L)

        override val currentUserId: Flow<Int?> = currentUserIdState.asStateFlow()
        override val sessionTimeout: Flow<Long> = sessionTimeoutState.asStateFlow()

        override suspend fun saveCurrentUserId(userId: Int) {
            currentUserIdState.value = userId
        }

        override suspend fun clearCurrentUser() {
            currentUserIdState.value = null
        }
    }

    private object NoOpDataStoreManager : DataStoreManager {
        override fun getSystemPrefs(): Flow<SystemPreferences> = emptyFlow()

        override fun usePreferencesByUserId(userId: Int) = Unit

        override suspend fun updateSystemPrefs(
            transform: suspend (t: SystemPreferences) -> SystemPreferences
        ) = Unit

        override fun <T : UserPreferences> getUserPrefs(
            key: Preferences.Key<String>,
            type: KClass<T>
        ): Flow<T> {
            return emptyFlow()
        }

        override suspend fun <T : UserPreferences> updateUserPrefs(
            key: Preferences.Key<String>,
            type: KClass<T>,
            transform: suspend (T) -> T
        ) = Unit

        override suspend fun deleteAllUserRelatedFiles(userId: Int) = Unit
    }
}
