package com.flixclusive.data.provider.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.UserSessionDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheCleanupScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userSessionDataStore: UserSessionDataStore,
    private val appDispatchers: AppDispatchers,
) {
    private var job: Job? = null

    @OptIn(FlowPreview::class)
    fun start() {
        if (job?.isActive == true) return

        job = appDispatchers.ioScope.launch {
            var previousUserId: String? = null

            userSessionDataStore.currentUserId
                .filterNotNull()
                .debounce(2000L)
                .collectLatest { userId ->
                    if (previousUserId != null && previousUserId != userId) {
                        cancel(previousUserId!!)
                    }

                    previousUserId = userId
                    schedule(userId)
                }
        }
    }

    private fun schedule(userId: String) {
        val request = PeriodicWorkRequestBuilder<CacheCleanupWorker>(24, TimeUnit.HOURS)
            .setInputData(workDataOf(CacheCleanupWorker.INPUT_USER_ID to userId))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueWorkName(userId),
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(userId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(userId))
    }

    private fun uniqueWorkName(userId: String) = "cache_cleanup_$userId"
}
