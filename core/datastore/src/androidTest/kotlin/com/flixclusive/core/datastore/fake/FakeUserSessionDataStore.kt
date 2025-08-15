package com.flixclusive.core.datastore.fake

import com.flixclusive.core.datastore.UserSessionDataStore
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeUserSessionDataStore : UserSessionDataStore {
    private val _currentUserId = MutableStateFlow<Int?>(null)
    private val _sessionTimeout = MutableStateFlow(0L)

    override val currentUserId = _currentUserId
    override val sessionTimeout = _sessionTimeout

    override suspend fun saveCurrentUserId(userId: Int) {
        _currentUserId.value = userId
        _sessionTimeout.value = System.currentTimeMillis() + 4 * 60 * 60 * 1000L
    }

    override suspend fun clearCurrentUser() {
        _currentUserId.value = null
        _sessionTimeout.value = 0L
    }

    fun setUserId(userId: Int?) {
        _currentUserId.value = userId
    }
}
