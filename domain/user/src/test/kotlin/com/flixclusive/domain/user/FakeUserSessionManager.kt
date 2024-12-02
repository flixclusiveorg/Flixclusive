package com.flixclusive.domain.user

import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class FakeUserSessionManager(
    private val users: List<User> = emptyList(),
    private val dataStore: FakeUserSessionDataStore
) : UserSessionManager {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun restoreSession() {
        val userId = dataStore.currentUserId.first()
        _currentUser.value = userId?.let { users.find { it.id == userId } } // Assuming a User object with id and name
    }

    override suspend fun signIn(user: User) {
        _currentUser.value = user
        dataStore.saveCurrentUserId(user.id)
    }

    override suspend fun signOut() {
        _currentUser.value = null
        dataStore.clearCurrentUser()
    }
}
