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
import com.flixclusive.data.backup.work.util.BackupWorkFile
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

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

            val dataPreferences = dataStoreManager
                .getUserPrefs(UserPreferences.DATA_PREFS_KEY, type = DataPreferences::class)
                .first()

            val maxBackups = dataPreferences.maxBackups.coerceAtLeast(1)
            val userBackupDir = BackupWorkFile.getUserBackupDirectory(userId).apply { mkdirs() }
            val outputFile = selectBackupOutputFile(
                userBackupDir = userBackupDir,
                maxBackups = maxBackups,
            )

            val outputUri = inputData.getString(BackupWorkConstants.INPUT_URI)
                ?.takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
                ?: Uri.fromFile(outputFile)

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


                BackupWorkFile.writeBackupResult(
                    file = BackupWorkFile.getLastBackupResultFile(
                        userId = userId,
                        fileName = BackupWorkConstants.LAST_CREATE_RESULT_FILE_NAME,
                    ),
                    result = result,
                )

                trimOldBackups(
                    userBackupDir = userBackupDir,
                    maxBackups = maxBackups,
                )

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

    private fun selectBackupOutputFile(userBackupDir: File, maxBackups: Int): File {
        val existing = userBackupDir
            .listFiles()
            ?.filter { it.isFile && it.extension == FileConstants.BACKUP_FILE_EXTENSION }
            .orEmpty()

        val target = if (existing.size >= maxBackups) {
            existing.minByOrNull { it.lastModified() }
        } else {
            null
        }

        return target ?: File(
            userBackupDir,
            "${BackupWorkConstants.BACKUP_FILE_PREFIX}${System.currentTimeMillis()}.${FileConstants.BACKUP_FILE_EXTENSION}",
        )
    }

    private fun trimOldBackups(userBackupDir: File, maxBackups: Int) {
        val backups = userBackupDir
            .listFiles()
            ?.filter {
                it.isFile
                    && it.extension == FileConstants.BACKUP_FILE_EXTENSION
                    && it.name.startsWith(BackupWorkConstants.BACKUP_FILE_PREFIX)
            }
            ?.sortedBy { it.lastModified() }
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
