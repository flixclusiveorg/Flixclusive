package com.flixclusive.data.backup.work

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoBackupScheduler @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val dataStoreManager: DataStoreManager,
    private val backupWorkManager: BackupWorkManager,
    private val appDispatchers: AppDispatchers,
) {

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return

        job = appDispatchers.ioScope.launch {
            var previousUserId: Int? = null

            userSessionDataStore.currentUserId
                .distinctUntilChanged()
                .collectLatest { currentUserId ->
                    if (previousUserId != null && previousUserId != currentUserId) {
                        backupWorkManager.cancelPeriodicAutoBackup(previousUserId!!)
                    }

                    previousUserId = currentUserId

                    if (currentUserId == null) return@collectLatest

                    dataStoreManager.usePreferencesByUserId(currentUserId)

                    dataStoreManager
                        .getUserPrefs(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
                        .map { it.autoBackupFrequencyDays }
                        .distinctUntilChanged()
                        .collectLatest { frequencyDays ->
                            backupWorkManager.syncPeriodicAutoBackup(
                                userId = currentUserId,
                                frequencyDays = frequencyDays,
                            )
                        }
                }
        }
    }
}
