package com.flixclusive.domain.user

import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.data.user.UserRepository
import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUserSessionManager
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val userSessionDataStore: UserSessionDataStore,
    ) : UserSessionManager {
        override val currentUser: StateFlow<User?> =
            userSessionDataStore.currentUserId
                .filterNotNull()
                .flatMapLatest { userRepository.observeUser(it) }
                .stateIn(
                    scope = AppDispatchers.Default.scope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        override suspend fun restoreSession() {
            withIOContext {
                // Attempt to restore user from last saved session
                val savedUserId = userSessionDataStore.currentUserId.first()
                val sessionTimeout = userSessionDataStore.sessionTimeout.first()

                if (sessionTimeout < System.currentTimeMillis()) {
                    return@withIOContext signOut()
                }

                savedUserId?.let { userId ->
                    userRepository
                        .getUser(id = userId)
                        ?.let { signIn(it) }
                        ?: signOut()
                }
            }
        }

        override suspend fun signIn(user: User) {
            withIOContext {
                userSessionDataStore.saveCurrentUserId(user.id)
            }
        }

        override suspend fun signOut() {
            withIOContext {
                userSessionDataStore.clearCurrentUser()
            }
        }
    }
