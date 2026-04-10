package com.flixclusive.data.backup.work.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.backup.di.BackupWorkerEntryPoint
import com.flixclusive.data.backup.repository.BackupResult
import com.flixclusive.data.backup.work.util.BackupWorkConstants
import com.flixclusive.data.backup.work.util.BackupWorkFiles
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
            val userBackupDir = BackupWorkFiles.getUserBackupDirectory(userId).apply { mkdirs() }
            val outputFile = selectBackupOutputFile(
                userBackupDir = userBackupDir,
                maxBackups = maxBackups,
            )

            runCatching {
                val options = dataPreferences.autoBackupOptions
                val result = entryPoint.backupRepository().create(
                    uri = Uri.fromFile(outputFile),
                    options = options,
                )

                writeBackupResult(
                    userId = userId,
                    result = result,
                )

                trimOldBackups(
                    userBackupDir = userBackupDir,
                    maxBackups = maxBackups,
                )

                Result.success()
            }.getOrElse { error ->
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
            ?.filter { it.isFile && it.extension == BackupWorkConstants.BACKUP_FILE_EXTENSION }
            .orEmpty()

        val target = if (existing.size >= maxBackups) {
            existing.minByOrNull { it.lastModified() }
        } else {
            null
        }

        return target ?: File(
            userBackupDir,
            "backup-${System.currentTimeMillis()}.${BackupWorkConstants.BACKUP_FILE_EXTENSION}",
        )
    }

    private fun trimOldBackups(userBackupDir: File, maxBackups: Int) {
        val backups = userBackupDir
            .listFiles()
            ?.filter { it.isFile && it.extension == BackupWorkConstants.BACKUP_FILE_EXTENSION }
            ?.sortedBy { it.lastModified() }
            .orEmpty()

        if (backups.size <= maxBackups) return

        val toDelete = backups.take(backups.size - maxBackups)
        toDelete.forEach { it.delete() }
    }

    private fun writeBackupResult(userId: Int, result: BackupResult) {
        val file = BackupWorkFiles.getLastBackupResultFile(userId)
        file.parentFile?.mkdirs()
        file.writeText(Json.encodeToString(BackupResult.serializer(), result))
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
