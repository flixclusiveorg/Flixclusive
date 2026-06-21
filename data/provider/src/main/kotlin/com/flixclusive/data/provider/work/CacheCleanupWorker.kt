package com.flixclusive.data.provider.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class CacheCleanupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val userId = inputData
            .getString(INPUT_USER_ID)
            ?.takeIf { it.isNotBlank() }
            ?: return Result.failure(
                workDataOf(OUTPUT_ERROR_MESSAGE to "Missing '$INPUT_USER_ID'")
            )

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            CacheCleanupWorkerEntryPoint::class.java,
        )

        return withContext(entryPoint.appDispatchers().io) {
            val dataStoreManager = entryPoint.dataStoreManager()
            dataStoreManager.usePreferencesByUserId(userId)

            val dataPrefs = dataStoreManager
                .getUserPrefsAsFlow(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
                .first()

            val retentionMs = dataPrefs.deadLinkRetentionDays.toLong() * 24L * 60L * 60L * 1000L
            val cutoffTimestamp = System.currentTimeMillis() - retentionMs

            entryPoint.cachedMediaLinkDao().deleteExpiredDeadLinks(
                ownerId = userId,
                cutoffTimestamp = cutoffTimestamp,
            )

            entryPoint.cachedMediaLinkDao().deleteThirdPartyOnlyLinks(userId)

            Result.success()
        }
    }

    companion object {
        const val INPUT_USER_ID = "user_id"
        private const val OUTPUT_ERROR_MESSAGE = "error_message"
    }
}
