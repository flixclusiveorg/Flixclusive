package com.flixclusive.domain.user

import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.data.user.UserRepository
import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUserSessionManager @Inject constructor(
    private val userRepository: UserRepository,
    private val dataStoreManager: UserSessionDataStore
) : UserSessionManager {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun restoreSession() {
        withIOContext {
            // Attempt to restore user from last saved session
            val savedUserId = dataStoreManager.currentUserId.first()
            val sessionTimeout = dataStoreManager.sessionTimeout.first()

            if (sessionTimeout < System.currentTimeMillis()) {
                return@withIOContext signOut()
            }

            savedUserId?.let { userId ->
                userRepository.getUser(id = userId)
                    ?.let { signIn(it) }
                    ?: signOut()
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