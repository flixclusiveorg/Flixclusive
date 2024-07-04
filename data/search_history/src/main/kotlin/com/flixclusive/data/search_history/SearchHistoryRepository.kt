package com.flixclusive.data.search_history

import com.flixclusive.model.database.SearchHistory
import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    suspend fun insert(item: SearchHistory)

    suspend fun remove(id: Int, ownerId: Int = 1)

    fun getAllItemsInFlow(ownerId: Int = 1): Flow<List<SearchHistory>>

    suspend fun clearAll(ownerId: Int = 1)
}
