package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.SearchHistoryDao
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.data.database.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    private val appDispatchers: AppDispatchers,
) : SearchHistoryRepository {
    override suspend fun insert(item: SearchHistory): Int {
        return withContext(appDispatchers.io) {
            searchHistoryDao.insert(item).toInt()
        }
    }

    override suspend fun remove(id: Int) {
        return withContext(appDispatchers.io) {
            searchHistoryDao.delete(id = id)
        }
    }

    override fun getAllItemsInFlow(ownerId: Int): Flow<List<SearchHistory>> {
        return searchHistoryDao.getAll(ownerId)
    }

    override suspend fun clearAll(ownerId: Int) {
        return withContext(appDispatchers.io) {
                searchHistoryDao.deleteAll(ownerId)
        }
    }
}
