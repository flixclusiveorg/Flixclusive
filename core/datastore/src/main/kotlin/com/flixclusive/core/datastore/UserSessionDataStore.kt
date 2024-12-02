package com.flixclusive.core.datastore

import kotlinx.coroutines.flow.Flow

const val CURRENT_USER_ID_KEY = "user_session_id"

interface UserSessionDataStore {
    val currentUserId: Flow<Int?>
    suspend fun saveCurrentUserId(userId: Int)
    suspend fun clearCurrentUser()
}