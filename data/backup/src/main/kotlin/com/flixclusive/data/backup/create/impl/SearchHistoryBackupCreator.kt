package com.flixclusive.data.backup.create.impl

import com.flixclusive.core.database.dao.SearchHistoryDao
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.create.BackupCreator
import com.flixclusive.data.backup.model.BackupSearchHistory
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class SearchHistoryBackupCreator @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupCreator<BackupSearchHistory> {
    override suspend fun invoke(): Result<List<BackupSearchHistory>> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()
            val searchHistory = searchHistoryDao.getAll(userId)

            searchHistory.map { history ->
                BackupSearchHistory(
                    query = history.query,
                    createdAt = history.createdAt.time,
                    updatedAt = history.updatedAt.time,
                )
            }
        }
    }
}
