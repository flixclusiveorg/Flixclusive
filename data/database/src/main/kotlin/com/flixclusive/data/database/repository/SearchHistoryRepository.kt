package com.flixclusive.data.database.repository

import com.flixclusive.core.database.entity.search.SearchHistory
import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    suspend fun insert(item: SearchHistory): Int

    suspend fun remove(id: Int)

    fun getAllItemsInFlow(ownerId: String): Flow<List<SearchHistory>>

    suspend fun clearAll(ownerId: String)
}
