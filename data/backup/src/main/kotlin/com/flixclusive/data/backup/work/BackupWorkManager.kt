package com.flixclusive.data.backup.work

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.model.user.BackupOptions
import com.flixclusive.data.backup.repository.BackupResult
import com.flixclusive.data.backup.work.util.BackupWorkConstants
import com.flixclusive.data.backup.work.util.BackupWorkResultStore
import com.flixclusive.data.backup.work.worker.BackupCreateWorker
import com.flixclusive.data.backup.work.worker.BackupRestoreWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupWorkManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appDispatchers: AppDispatchers,
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    fun enqueueCreate(userId: Int): String {
        val uniqueName = BackupWorkConstants.UNIQUE_BACKUP_CREATE_PREFIX + userId

        val request = OneTimeWorkRequestBuilder<BackupCreateWorker>()
            .setInputData(
                workDataOf(
                    BackupWorkConstants.INPUT_USER_ID to userId,
                )
            )
            .addTag(BackupWorkConstants.TAG_BACKUP_CREATE)
            .addTag(BackupWorkConstants.TAG_BACKUP_CREATE_USER_PREFIX + userId)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            request,
        )

        return uniqueName
    }

    fun enqueueCreate(
        userId: Int,
        uri: Uri,
        options: BackupOptions = BackupOptions(),
    ): String {
        val uniqueName = BackupWorkConstants.UNIQUE_BACKUP_CREATE_PREFIX + userId

        val request = OneTimeWorkRequestBuilder<BackupCreateWorker>()
            .setInputData(
                workDataOf(
                    BackupWorkConstants.INPUT_USER_ID to userId,
                    BackupWorkConstants.INPUT_URI to uri.toString(),
                    BackupWorkConstants.INPUT_INCLUDE_LIBRARY to options.includeLibrary,
                    BackupWorkConstants.INPUT_INCLUDE_WATCH_PROGRESS to options.includeWatchProgress,
                    BackupWorkConstants.INPUT_INCLUDE_SEARCH_HISTORY to options.includeSearchHistory,
                    BackupWorkConstants.INPUT_INCLUDE_PREFERENCES to options.includePreferences,
                    BackupWorkConstants.INPUT_INCLUDE_PROVIDERS to options.includeProviders,
                    BackupWorkConstants.INPUT_INCLUDE_REPOSITORIES to options.includeRepositories,
                )
            )
            .addTag(BackupWorkConstants.TAG_BACKUP_CREATE)
            .addTag(BackupWorkConstants.TAG_BACKUP_CREATE_USER_PREFIX + userId)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            request,
        )

        return uniqueName
    }

    fun enqueueRestore(
        userId: Int,
        uri: Uri,
        options: BackupOptions = BackupOptions(),
    ): String {
        val uniqueName = BackupWorkConstants.UNIQUE_BACKUP_RESTORE_PREFIX + userId

        val request = OneTimeWorkRequestBuilder<BackupRestoreWorker>()
            .setInputData(
                workDataOf(
                    BackupWorkConstants.INPUT_USER_ID to userId,
                    BackupWorkConstants.INPUT_URI to uri.toString(),
                    BackupWorkConstants.INPUT_INCLUDE_LIBRARY to options.includeLibrary,
                    BackupWorkConstants.INPUT_INCLUDE_WATCH_PROGRESS to options.includeWatchProgress,
                    BackupWorkConstants.INPUT_INCLUDE_SEARCH_HISTORY to options.includeSearchHistory,
                    BackupWorkConstants.INPUT_INCLUDE_PREFERENCES to options.includePreferences,
                    BackupWorkConstants.INPUT_INCLUDE_PROVIDERS to options.includeProviders,
                    BackupWorkConstants.INPUT_INCLUDE_REPOSITORIES to options.includeRepositories,
                )
            )
            .addTag(BackupWorkConstants.TAG_BACKUP_RESTORE)
            .addTag(BackupWorkConstants.TAG_BACKUP_RESTORE_USER_PREFIX + userId)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            uniqueName,
            ExistingWorkPolicy.KEEP,
            request,
        )

        return uniqueName
    }

    fun syncPeriodicAutoBackup(userId: Int, frequencyDays: Int) {
        val uniqueName = BackupWorkConstants.UNIQUE_AUTO_BACKUP_CREATE_PREFIX + userId
        val clampedDays = frequencyDays.coerceIn(0, 30)

        if (clampedDays <= 0) {
            cancelPeriodicAutoBackup(userId)
            return
        }

        val request = PeriodicWorkRequestBuilder<BackupCreateWorker>(
            clampedDays.toLong(),
            TimeUnit.DAYS,
        )
            .setInputData(
                workDataOf(
                    BackupWorkConstants.INPUT_USER_ID to userId,
                )
            )
            .addTag(BackupWorkConstants.TAG_AUTO_BACKUP_CREATE)
            .addTag(BackupWorkConstants.TAG_BACKUP_CREATE)
            .addTag(BackupWorkConstants.TAG_BACKUP_CREATE_USER_PREFIX + userId)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    fun cancelPeriodicAutoBackup(userId: Int) {
        val uniqueName = BackupWorkConstants.UNIQUE_AUTO_BACKUP_CREATE_PREFIX + userId
        workManager.cancelUniqueWork(uniqueName)
    }

    fun observeUniqueWork(uniqueWorkName: String): Flow<WorkInfo?> {
        return workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName)
            .map { infos -> infos.lastOrNull() }
    }

    suspend fun readLastCreateResult(userId: Int): BackupResult {
        return withContext(appDispatchers.io) {
            BackupWorkResultStore.read(
                context = context,
                userId = userId,
                fileName = BackupWorkConstants.LAST_CREATE_RESULT_FILE_NAME,
            )
        }
    }

    suspend fun readLastRestoreResult(userId: Int): BackupResult {
        return withContext(appDispatchers.io) {
            BackupWorkResultStore.read(
                context = context,
                userId = userId,
                fileName = BackupWorkConstants.LAST_RESTORE_RESULT_FILE_NAME,
            )
        }
    }
}
