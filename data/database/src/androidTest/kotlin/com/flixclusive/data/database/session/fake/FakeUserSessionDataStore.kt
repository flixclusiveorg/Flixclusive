package com.flixclusive.data.database.session.fake

import com.flixclusive.core.datastore.UserSessionDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeUserSessionDataStore(
    testUserId: Int? = null,
    testSessionTimeout: Long = 0L
) : UserSessionDataStore {
    private val _currentUserId: MutableStateFlow<Int?> = MutableStateFlow(testUserId)
    override val currentUserId: Flow<Int?> get() = _currentUserId

    private val _sessionTimeout = MutableStateFlow(testSessionTimeout)
    override val sessionTimeout: Flow<Long> get() = _sessionTimeout

    override suspend fun saveCurrentUserId(userId: Int) {
        _currentUserId.value = userId
        val sessionTimeoutMillis = System.currentTimeMillis() + 4 * 60 * 60 * 1000L
        _sessionTimeout.value = sessionTimeoutMillis
    }

    override suspend fun clearCurrentUser() {
        _currentUserId.value = null
        _sessionTimeout.value = 0L
    }
}
