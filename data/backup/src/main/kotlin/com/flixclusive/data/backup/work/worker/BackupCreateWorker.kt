package com.flixclusive.data.backup.work.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flixclusive.core.common.file.FileConstants
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.backup.di.BackupWorkerEntryPoint
import com.flixclusive.data.backup.work.util.BackupWorkConstants
import com.flixclusive.data.backup.work.util.BackupWorkResultStore
import dagger.hilt.android.EntryPointAccessors
import com.hippo.unifile.UniFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class BackupCreateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val userId = inputData.getInt(BackupWorkConstants.INPUT_USER_ID, -1)
        if (userId <= 0) {
            return Result.failure(
                workDataOf(
                    BackupWorkConstants.OUTPUT_ERROR_MESSAGE to "Missing '${BackupWorkConstants.INPUT_USER_ID}'",
                )
            )
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            BackupWorkerEntryPoint::class.java,
        )

        return withContext(entryPoint.appDispatchers().io) {
            val workManager = WorkManager.getInstance(applicationContext)

            val hasOtherCreateRunning = runCatching {
                hasOtherWorkRunning(
                    workManager = workManager,
                    tag = BackupWorkConstants.TAG_BACKUP_CREATE_USER_PREFIX + userId,
                )
            }.getOrElse { return@withContext Result.retry() }

            if (hasOtherCreateRunning) {
                return@withContext Result.retry()
            }

            val hasRestorePendingOrRunning = runCatching {
                hasWorkPendingOrRunning(
                    workManager = workManager,
                    tag = BackupWorkConstants.TAG_BACKUP_RESTORE_USER_PREFIX + userId,
                )
            }.getOrElse { return@withContext Result.retry() }

            if (hasRestorePendingOrRunning) {
                return@withContext Result.retry()
            }

            val userSessionDataStore = entryPoint.userSessionDataStore()
            val currentUserId = userSessionDataStore.currentUserId.first()
                ?: return@withContext if (runAttemptCount < 3) Result.retry() else Result.success()

            if (currentUserId != userId) {
                return@withContext Result.success()
            }

            val dataStoreManager = entryPoint.dataStoreManager()
            dataStoreManager.usePreferencesByUserId(userId)

            val storageDirectoryUri = dataStoreManager.getSystemPrefs()
                .first()
                .storageDirectoryUri
                ?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(
                    workDataOf(
                        BackupWorkConstants.OUTPUT_ERROR_MESSAGE to "Backup location is not set",
                    ),
                )

            val dataPreferences = dataStoreManager
                .getUserPrefs(UserPreferences.DATA_PREFS_KEY, type = DataPreferences::class)
                .first()

            val maxBackups = dataPreferences.maxBackups.coerceAtLeast(1)

            val explicitOutputUri = inputData.getString(BackupWorkConstants.INPUT_URI)
                ?.takeIf { it.isNotBlank() }
                ?.let(Uri::parse)

            val userBackupDirForTrim: UniFile?
            val outputUri = if (explicitOutputUri != null) {
                userBackupDirForTrim = null
                explicitOutputUri
            } else {
                val root = UniFile.fromUri(applicationContext, Uri.parse(storageDirectoryUri))
                    ?: return@withContext Result.failure(
                        workDataOf(
                            BackupWorkConstants.OUTPUT_ERROR_MESSAGE to "Unable to access backup location",
                        ),
                    )

                if (!root.exists() || !root.isDirectory || !root.canWrite()) {
                    return@withContext Result.failure(
                        workDataOf(
                            BackupWorkConstants.OUTPUT_ERROR_MESSAGE to "Backup location is not writable",
                        ),
                    )
                }

                val backupsDir = root.findFile("backups") ?: root.createDirectory("backups")
                val userBackupDir = backupsDir?.findFile("user-$userId")
                    ?: backupsDir?.createDirectory("user-$userId")
                    ?: return@withContext Result.failure(
                        workDataOf(
                            BackupWorkConstants.OUTPUT_ERROR_MESSAGE to "Unable to create backup folder",
                        ),
                    )

                userBackupDirForTrim = userBackupDir

                val outputFile = selectBackupOutputFile(
                    userBackupDir = userBackupDir,
                    maxBackups = maxBackups,
                )

                outputFile.uri
            }

            runCatching {
                val defaultOptions = dataPreferences.autoBackupOptions
                val options = defaultOptions.copy(
                    includeLibrary = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_LIBRARY, defaultOptions.includeLibrary),
                    includeWatchProgress = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_WATCH_PROGRESS, defaultOptions.includeWatchProgress),
                    includeSearchHistory = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_SEARCH_HISTORY, defaultOptions.includeSearchHistory),
                    includePreferences = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_PREFERENCES, defaultOptions.includePreferences),
                    includeProviders = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_PROVIDERS, defaultOptions.includeProviders),
                    includeRepositories = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_REPOSITORIES, defaultOptions.includeRepositories),
                )
                val result = entryPoint.backupRepository().create(
                    uri = outputUri,
                    options = options,
                )

                BackupWorkResultStore.write(
                    context = applicationContext,
                    userId = userId,
                    fileName = BackupWorkConstants.LAST_CREATE_RESULT_FILE_NAME,
                    result = result,
                )

                userBackupDirForTrim?.let { trimOldBackups(userBackupDir = it, maxBackups = maxBackups) }

                Result.success()
            }.getOrElse { error ->
                errorLog(error)
                Result.failure(
                    workDataOf(
                        BackupWorkConstants.OUTPUT_ERROR_MESSAGE to (error.message ?: error::class.java.simpleName),
                    )
                )
            }
        }
    }

    private fun selectBackupOutputFile(userBackupDir: UniFile, maxBackups: Int): UniFile {
        val backups = userBackupDir.listFiles()
            ?.asSequence()
            ?.filter { it.isFile }
            ?.filter { it.name?.startsWith(BackupWorkConstants.BACKUP_FILE_PREFIX) == true }
            ?.filter { it.name?.endsWith(".${FileConstants.BACKUP_FILE_EXTENSION}") == true }
            ?.sortedBy { it.lastModified() }
            ?.toList()
            .orEmpty()

        val target = if (backups.size >= maxBackups) backups.firstOrNull() else null

        return target ?: userBackupDir.createFile(
            "${BackupWorkConstants.BACKUP_FILE_PREFIX}${System.currentTimeMillis()}.${FileConstants.BACKUP_FILE_EXTENSION}",
        ) ?: error("Unable to create backup file")
    }

    private fun trimOldBackups(userBackupDir: UniFile, maxBackups: Int) {
        val backups = userBackupDir.listFiles()
            ?.asSequence()
            ?.filter { it.isFile }
            ?.filter { it.name?.startsWith(BackupWorkConstants.BACKUP_FILE_PREFIX) == true }
            ?.filter { it.name?.endsWith(".${FileConstants.BACKUP_FILE_EXTENSION}") == true }
            ?.sortedBy { it.lastModified() }
            ?.toList()
            .orEmpty()

        if (backups.size <= maxBackups) return

        val toDelete = backups.take(backups.size - maxBackups)
        toDelete.forEach { it.delete() }
    }

    private fun hasOtherWorkRunning(workManager: WorkManager, tag: String): Boolean {
        val infos = workManager.getWorkInfosByTag(tag).get()
        return infos.any { it.id != id && it.state == WorkInfo.State.RUNNING }
    }

    private fun hasWorkPendingOrRunning(workManager: WorkManager, tag: String): Boolean {
        val infos = workManager.getWorkInfosByTag(tag).get()
        return infos.any { it.state.isPendingOrRunningWorkState() }
    }

    private fun WorkInfo.State.isPendingOrRunningWorkState(): Boolean {
        return this == WorkInfo.State.ENQUEUED ||
            this == WorkInfo.State.RUNNING ||
            this == WorkInfo.State.BLOCKED
    }
}
