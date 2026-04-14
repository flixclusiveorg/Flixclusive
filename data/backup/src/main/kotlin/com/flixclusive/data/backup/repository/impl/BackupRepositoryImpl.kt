package com.flixclusive.data.backup.repository.impl

import android.content.Context
import android.net.Uri
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.provider.ProviderFile.getProvidersPath
import com.flixclusive.core.common.provider.ProviderFile.getProvidersSettingsPath
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.BackupOptions
import com.flixclusive.data.backup.create.BackupCreator
import com.flixclusive.data.backup.model.Backup
import com.flixclusive.data.backup.model.BackupLibraryList
import com.flixclusive.data.backup.model.BackupPreference
import com.flixclusive.data.backup.model.BackupProvider
import com.flixclusive.data.backup.model.BackupProviderRepository
import com.flixclusive.data.backup.model.BackupSearchHistory
import com.flixclusive.data.backup.model.BackupWatchProgress
import com.flixclusive.data.backup.repository.BackupRepository
import com.flixclusive.data.backup.repository.BackupResult
import com.flixclusive.data.backup.repository.NoDataToBackupException
import com.flixclusive.data.backup.restore.BackupRestorer
import com.flixclusive.data.backup.util.BackupUtil
import com.flixclusive.data.backup.util.BackupUtil.decodeFromUriAndRestoreFiles
import com.flixclusive.data.backup.validate.BackupValidationMode
import com.flixclusive.data.backup.validate.BackupValidator
import com.hippo.unifile.UniFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalSerializationApi::class)
internal class BackupRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appDispatchers: AppDispatchers,
    private val userSessionDataStore: UserSessionDataStore,

    private val libraryListBackupValidator: BackupValidator<BackupLibraryList>,
    private val preferenceBackupValidator: BackupValidator<BackupPreference>,
    private val watchProgressBackupValidator: BackupValidator<BackupWatchProgress>,
    private val searchHistoryBackupValidator: BackupValidator<BackupSearchHistory>,
    private val providerBackupValidator: BackupValidator<BackupProvider>,
    private val repositoryBackupValidator: BackupValidator<BackupProviderRepository>,
    // ==
    private val libraryListBackupCreator: BackupCreator<BackupLibraryList>,
    private val preferenceBackupCreator: BackupCreator<BackupPreference>,
    private val watchProgressBackupCreator: BackupCreator<BackupWatchProgress>,
    private val searchHistoryBackupCreator: BackupCreator<BackupSearchHistory>,
    private val providerBackupCreator: BackupCreator<BackupProvider>,
    private val repositoryBackupCreator: BackupCreator<BackupProviderRepository>,
    // ==
    private val libraryListBackupRestorer: BackupRestorer<BackupLibraryList>,
    private val preferenceBackupRestorer: BackupRestorer<BackupPreference>,
    private val watchProgressBackupRestorer: BackupRestorer<BackupWatchProgress>,
    private val searchHistoryBackupRestorer: BackupRestorer<BackupSearchHistory>,
    private val providerBackupRestorer: BackupRestorer<BackupProvider>,
    private val repositoryBackupRestorer: BackupRestorer<BackupProviderRepository>,
) : BackupRepository {
    override suspend fun create(uri: Uri, options: BackupOptions): BackupResult {
        return withContext(appDispatchers.io) {
            val file = UniFile.fromUri(context, uri)
                ?: error("Unable to open output stream for uri: $uri")

            val backup = Backup(
                libraryLists = if (options.includeLibrary) libraryListBackupCreator().getOrThrow() else emptyList(),
                preferences = if (options.includePreferences) preferenceBackupCreator().getOrThrow() else emptyList(),
                watchProgressList = if (options.includeWatchProgress) watchProgressBackupCreator().getOrThrow() else emptyList(),
                searchHistory = if (options.includeSearchHistory) searchHistoryBackupCreator().getOrThrow() else emptyList(),
                providers = if (options.includeProviders) providerBackupCreator().getOrThrow() else emptyList(),
                repositories = if (options.includeRepositories) repositoryBackupCreator().getOrThrow() else emptyList(),
            )

            val result = BackupResult(
                missingLibraryLists = if (options.includeLibrary) libraryListBackupValidator(
                    backup = backup.libraryLists,
                    mode = BackupValidationMode.CREATE,
                ).getOrThrow() else emptySet(),
                missingPreferences = if (options.includePreferences) preferenceBackupValidator(
                    backup = backup.preferences,
                    mode = BackupValidationMode.CREATE,
                ).getOrThrow() else emptySet(),
                missingWatchProgress = if (options.includeWatchProgress) watchProgressBackupValidator(
                    backup = backup.watchProgressList,
                    mode = BackupValidationMode.CREATE,
                ).getOrThrow() else emptySet(),
                missingSearchHistory = if (options.includeSearchHistory) searchHistoryBackupValidator(
                    backup = backup.searchHistory,
                    mode = BackupValidationMode.CREATE,
                ).getOrThrow() else emptySet(),
                missingProviders = if (options.includeProviders) providerBackupValidator(
                    backup = backup.providers,
                    mode = BackupValidationMode.CREATE,
                ).getOrThrow() else emptySet(),
                missingProviderRepositories = if (options.includeRepositories) repositoryBackupValidator(
                    backup = backup.repositories,
                    mode = BackupValidationMode.CREATE,
                ).getOrThrow() else emptySet(),
            )

            val bytes = ProtoBuf.encodeToByteArray(Backup.serializer(), backup)
            if (bytes.isEmpty()) throw NoDataToBackupException()

            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val userProvidersFolder = File(context.getProvidersPath(userId))
            val userProvidersSettingsFolder = File(context.getProvidersSettingsPath(userId))

            file.openOutputStream()
                .also {
                    // Force overwrite old file
                    (it as? FileOutputStream)?.channel?.truncate(0)
                }
                .let { outputStream -> BufferedOutputStream(outputStream) }
                .use { bufferedOut ->
                    ZipOutputStream(bufferedOut).use { zip ->
                        zip.putNextEntry(ZipEntry(BackupUtil.ZIP_ENTRY_BACKUP))
                        zip.write(bytes)
                        zip.closeEntry()

                        if (options.includeProviders) {
                            if (userProvidersFolder.isDirectory) {
                                addFolderToZip(
                                    zip = zip,
                                    folder = userProvidersFolder,
                                    entryPrefix = BackupUtil.ZIP_ENTRY_PROVIDERS_PREFIX,
                                )
                            }

                            if (userProvidersSettingsFolder.isDirectory) {
                                addFolderToZip(
                                    zip = zip,
                                    folder = userProvidersSettingsFolder,
                                    entryPrefix = BackupUtil.ZIP_ENTRY_PROVIDERS_SETTINGS_PREFIX,
                                )
                            }
                        }
                    }
                }

            result
        }
    }

    override suspend fun restore(uri: Uri, options: BackupOptions): BackupResult {
        return withContext(appDispatchers.io) {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val backup = context.decodeFromUriAndRestoreFiles(
                restoreProviderFiles = options.includeProviders,
                userId = userId,
                uri = uri,
            )

            val includeRepositories = options.includeRepositories && backup.repositories.isNotEmpty()
            val includeProviders = options.includeProviders && includeRepositories && backup.providers.isNotEmpty()
            val includeLibrary = options.includeLibrary && backup.libraryLists.isNotEmpty()
            val includeWatchProgress = options.includeWatchProgress && includeLibrary && backup.watchProgressList.isNotEmpty()
            val includeSearchHistory = options.includeSearchHistory && backup.searchHistory.isNotEmpty()
            val includePreferences = options.includePreferences && backup.preferences.isNotEmpty()

            if (includeRepositories) {
                repositoryBackupRestorer(backup.repositories).getOrThrow()
            }

            if (includeProviders) {
                providerBackupRestorer(backup.providers).getOrThrow()
            }

            if (includeLibrary) {
                libraryListBackupRestorer(backup.libraryLists).getOrThrow()
            }

            if (includeWatchProgress) {
                watchProgressBackupRestorer(backup.watchProgressList).getOrThrow()
            }

            if (includeSearchHistory) {
                searchHistoryBackupRestorer(backup.searchHistory).getOrThrow()
            }

            if (includePreferences) {
                preferenceBackupRestorer(backup.preferences).getOrThrow()
            }

            BackupResult(
                missingLibraryLists = if (includeLibrary) libraryListBackupValidator(
                    backup = backup.libraryLists,
                    mode = BackupValidationMode.RESTORE,
                ).getOrThrow() else emptySet(),
                missingProviders = if (includeProviders) providerBackupValidator(
                    backup = backup.providers,
                    mode = BackupValidationMode.RESTORE,
                ).getOrThrow() else emptySet(),
                missingProviderRepositories = if (includeRepositories) repositoryBackupValidator(
                    backup = backup.repositories,
                    mode = BackupValidationMode.RESTORE,
                ).getOrThrow() else emptySet(),
                missingPreferences = if (includePreferences) preferenceBackupValidator(
                    backup = backup.preferences,
                    mode = BackupValidationMode.RESTORE,
                ).getOrThrow() else emptySet(),
                missingSearchHistory = if (includeSearchHistory) searchHistoryBackupValidator(
                    backup = backup.searchHistory,
                    mode = BackupValidationMode.RESTORE,
                ).getOrThrow() else emptySet(),
                missingWatchProgress = if (includeWatchProgress) watchProgressBackupValidator(
                    backup = backup.watchProgressList,
                    mode = BackupValidationMode.RESTORE,
                ).getOrThrow() else emptySet(),
            )
        }
    }

    private fun addFolderToZip(
        zip: ZipOutputStream,
        folder: File,
        entryPrefix: String
    ) {
        val basePath = folder.absolutePath.trimEnd(File.separatorChar)

        folder.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = file.absolutePath
                    .substring(basePath.length + 1)
                    .replace(File.separatorChar, '/')

                zip.putNextEntry(ZipEntry(entryPrefix + relativePath))
                file.inputStream().use { input ->
                    input.copyTo(zip)
                }
                zip.closeEntry()
            }
    }
}
