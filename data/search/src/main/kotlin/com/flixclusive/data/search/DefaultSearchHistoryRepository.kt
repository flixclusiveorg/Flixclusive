package com.flixclusive.data.search

import com.flixclusive.core.database.dao.SearchHistoryDao
import com.flixclusive.model.database.SearchHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class DefaultSearchHistoryRepository @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) : SearchHistoryRepository {
    override suspend fun insert(item: SearchHistory) {
        searchHistoryDao.insert(item)
    }

    override suspend fun remove(id: Int, ownerId: Int) {
        searchHistoryDao.deleteById(ownerId = ownerId, id = id)
    }

    override fun getAllItemsInFlow(ownerId: Int): Flow<List<SearchHistory>> {
        return searchHistoryDao.getSearchHistory(ownerId)
    }

    override suspend fun clearAll(ownerId: Int) {
        searchHistoryDao.clearAll(ownerId)
    }
}
