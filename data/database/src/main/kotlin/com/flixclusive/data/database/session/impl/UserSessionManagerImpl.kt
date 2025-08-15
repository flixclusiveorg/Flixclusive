package com.flixclusive.data.database.session.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.User
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.session.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserSessionManagerImpl
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val userSessionDataStore: UserSessionDataStore,
        private val appDispatchers: AppDispatchers,
    ) : UserSessionManager {
        override val currentUser: StateFlow<User?> =
            userSessionDataStore.currentUserId
                .flatMapLatest { it?.let(userRepository::observeUser) ?: flowOf(null) }
                .stateIn(
                    scope = CoroutineScope(appDispatchers.io + SupervisorJob()),
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        override suspend fun restoreSession() {
            withContext(appDispatchers.io) {
                val savedUserId = userSessionDataStore.currentUserId.first()!!
                val user = userRepository.getUser(id = savedUserId)!!
                signIn(user)
            }
        }

        override suspend fun hasOldSession(): Boolean {
            return withContext(appDispatchers.io) {
                val savedUserId = userSessionDataStore.currentUserId.first()
                val sessionTimeout = userSessionDataStore.sessionTimeout.first()

                if (savedUserId == null) return@withContext false
                if (sessionTimeout < System.currentTimeMillis()) return@withContext false

                val user = userRepository.getUser(id = savedUserId)
                if (user == null) return@withContext false

                return@withContext true
            }
        }

        override suspend fun signIn(user: User) {
            withContext(appDispatchers.io) {
                userSessionDataStore.saveCurrentUserId(user.id)
            }
        }

        override suspend fun signOut() {
            withContext(appDispatchers.io) {
                userSessionDataStore.clearCurrentUser()
            }
        }
    }
