package com.flixclusive.core.datastore

import kotlinx.coroutines.flow.Flow

const val CURRENT_USER_ID_KEY = "user_session_id"
const val SESSION_TIMEOUT_ID_KEY = "session_timeout"

interface UserSessionDataStore {
    val currentUserId: Flow<Int?>
    val sessionTimeout: Flow<Long>
    suspend fun saveCurrentUserId(userId: Int)
    suspend fun clearCurrentUser()
}