package com.flixclusive.data.search_history

import com.flixclusive.model.database.SearchHistory
import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    suspend fun insert(item: SearchHistory)

    suspend fun remove(id: Int, ownerId: Int)

    fun getAllItemsInFlow(ownerId: Int): Flow<List<SearchHistory>>

    suspend fun clearAll(ownerId: Int)
}
