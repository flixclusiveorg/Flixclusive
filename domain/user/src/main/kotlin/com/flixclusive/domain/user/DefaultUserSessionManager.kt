package com.flixclusive.domain.user

import com.flixclusive.core.datastore.DefaultUserSessionDataStore
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.data.user.UserRepository
import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DefaultUserSessionManager @Inject constructor(
    private val userRepository: UserRepository,
    private val dataStoreManager: DefaultUserSessionDataStore
) : UserSessionManager {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun restoreSession() {
        withIOContext {
            // Attempt to restore user from last saved session
            val savedUserId = dataStoreManager.currentUserId.first()

            savedUserId?.let { userId ->
                val user = userRepository.getUser(userId)
                _currentUser.value = user
            }
        }
    }

    override suspend fun signIn(user: User) {
        withIOContext {
            dataStoreManager.saveCurrentUserId(user.id)
            _currentUser.value = user
        }
    }

    override suspend fun signOut() {
        withIOContext {
            dataStoreManager.clearCurrentUser()
            _currentUser.value = null
        }
    }
}