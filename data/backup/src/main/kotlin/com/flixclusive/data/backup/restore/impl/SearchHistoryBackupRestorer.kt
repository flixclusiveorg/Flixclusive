package com.flixclusive.data.backup.restore.impl

import com.flixclusive.core.database.dao.SearchHistoryDao
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupSearchHistory
import com.flixclusive.data.backup.restore.BackupRestorer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

internal class SearchHistoryBackupRestorer @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupRestorer<BackupSearchHistory> {
    override suspend fun invoke(items: List<BackupSearchHistory>): Result<Unit> {
        return runCatching {
            val ownerId = userSessionDataStore.currentUserId.filterNotNull().first()

            searchHistoryDao.deleteAll(ownerId)

            items.forEach { history ->
                searchHistoryDao.insert(
                    SearchHistory(
                        query = history.query,
                        ownerId = ownerId,
                        createdAt = Date(history.createdAt),
                        updatedAt = Date(history.updatedAt),
                    )
                )
            }
        }
    }
}
