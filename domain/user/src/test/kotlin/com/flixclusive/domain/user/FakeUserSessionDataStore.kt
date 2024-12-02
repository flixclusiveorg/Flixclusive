package com.flixclusive.domain.user

import com.flixclusive.core.datastore.CURRENT_USER_ID_KEY
import com.flixclusive.core.datastore.UserSessionDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeUserSessionDataStore : UserSessionDataStore {
    private val preferences = mutableMapOf<String, Int?>()

    override val currentUserId: Flow<Int?> = flow {
        emit(preferences[CURRENT_USER_ID_KEY])
    }

    override suspend fun saveCurrentUserId(userId: Int) {
        preferences[CURRENT_USER_ID_KEY] = userId
    }

    override suspend fun clearCurrentUser() {
        preferences.remove(CURRENT_USER_ID_KEY)
    }
}
