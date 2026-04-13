package com.flixclusive.core.datastore

import kotlinx.coroutines.flow.Flow

interface UserSessionDataStore {
    val currentUserId: Flow<String?>
    val sessionTimeout: Flow<Long>
    suspend fun saveCurrentUserId(userId: String)
    suspend fun clearCurrentUser()
}
