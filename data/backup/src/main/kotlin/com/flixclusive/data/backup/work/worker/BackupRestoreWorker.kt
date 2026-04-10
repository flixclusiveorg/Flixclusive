package com.flixclusive.data.backup.work.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flixclusive.core.datastore.model.user.BackupOptions
import com.flixclusive.data.backup.di.BackupWorkerEntryPoint
import com.flixclusive.data.backup.repository.BackupResult
import com.flixclusive.data.backup.work.util.BackupWorkConstants
import com.flixclusive.data.backup.work.util.BackupWorkFiles
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class BackupRestoreWorker(
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

        val uriString = inputData.getString(BackupWorkConstants.INPUT_URI)
        if (uriString.isNullOrBlank()) {
            return Result.failure(
                workDataOf(
                    BackupWorkConstants.OUTPUT_ERROR_MESSAGE to "Missing '${BackupWorkConstants.INPUT_URI}'",
                )
            )
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            BackupWorkerEntryPoint::class.java,
        )

        return withContext(entryPoint.appDispatchers().io) {
            val workManager = WorkManager.getInstance(applicationContext)

            val hasOtherRestoreRunning = runCatching {
                hasOtherWorkRunning(
                    workManager = workManager,
                    tag = BackupWorkConstants.TAG_BACKUP_RESTORE_USER_PREFIX + userId,
                )
            }.getOrElse { return@withContext Result.retry() }

            if (hasOtherRestoreRunning) {
                return@withContext Result.retry()
            }

            val hasCreateRunning = runCatching {
                hasWorkRunning(
                    workManager = workManager,
                    tag = BackupWorkConstants.TAG_BACKUP_CREATE_USER_PREFIX + userId,
                )
            }.getOrElse { return@withContext Result.retry() }

            if (hasCreateRunning) {
                return@withContext Result.retry()
            }

            val currentUserId = entryPoint.userSessionDataStore().currentUserId.first()
            if (currentUserId == null) {
                return@withContext if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure(
                        workDataOf(
                            BackupWorkConstants.OUTPUT_ERROR_MESSAGE to "No active user session",
                        )
                    )
                }
            }

            if (currentUserId != userId) {
                return@withContext Result.failure(
                    workDataOf(
                        BackupWorkConstants.OUTPUT_ERROR_MESSAGE to "User session changed (expected user-$userId, current user-$currentUserId)",
                    )
                )
            }

            runCatching {
                val uri = Uri.parse(uriString)
                val options = BackupOptions(
                    includeLibrary = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_LIBRARY, true),
                    includeWatchProgress = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_WATCH_PROGRESS, true),
                    includeSearchHistory = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_SEARCH_HISTORY, true),
                    includePreferences = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_PREFERENCES, true),
                    includeProviders = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_PROVIDERS, true),
                    includeRepositories = inputData.getBoolean(BackupWorkConstants.INPUT_INCLUDE_REPOSITORIES, true),
                )

                val result = entryPoint.backupRepository().restore(
                    uri = uri,
                    options = options,
                )

                writeRestoreResult(
                    userId = userId,
                    result = result,
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

    private fun writeRestoreResult(userId: Int, result: BackupResult) {
        val file = BackupWorkFiles.getLastRestoreResultFile(
            context = applicationContext,
            userId = userId,
        )
        file.parentFile?.mkdirs()
        file.writeText(Json.encodeToString(BackupResult.serializer(), result))
    }

    private fun hasOtherWorkRunning(workManager: WorkManager, tag: String): Boolean {
        val infos = workManager.getWorkInfosByTag(tag).get()
        return infos.any { it.id != id && it.state == WorkInfo.State.RUNNING }
    }

    private fun hasWorkRunning(workManager: WorkManager, tag: String): Boolean {
        val infos = workManager.getWorkInfosByTag(tag).get()
        return infos.any { it.state == WorkInfo.State.RUNNING }
    }
}
