package com.flixclusive.domain.user

import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.core.util.coroutines.firstNotNull
import com.flixclusive.data.user.UserRepository
import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
                .flatMapLatest { it?.let(userRepository::observeUser) ?: flowOf(null) }
                .stateIn(
                    scope = AppDispatchers.IO.scope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        override suspend fun restoreSession() {
            withIOContext {
                val savedUserId = userSessionDataStore.currentUserId.firstNotNull()!!
                val user = userRepository.getUser(id = savedUserId)!!
                signIn(user)
            }
        }

        override suspend fun hasOldSession(): Boolean {
            return withIOContext {
                val savedUserId = userSessionDataStore.currentUserId.first()
                val sessionTimeout = userSessionDataStore.sessionTimeout.first()

                if (savedUserId == null) return@withIOContext false
                if (sessionTimeout < System.currentTimeMillis()) return@withIOContext false

                val user = userRepository.getUser(id = savedUserId)
                if (user == null) return@withIOContext false

                return@withIOContext true
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
