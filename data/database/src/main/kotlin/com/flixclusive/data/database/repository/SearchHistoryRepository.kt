package com.flixclusive.data.database.repository

import com.flixclusive.core.database.entity.SearchHistory
import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    suspend fun insert(item: SearchHistory)

    suspend fun remove(id: Int, ownerId: Int)

    fun getAllItemsInFlow(ownerId: Int): Flow<List<SearchHistory>>

    suspend fun clearAll(ownerId: Int)
}
