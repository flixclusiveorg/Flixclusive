package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.database.repository.UserAuthRepository
import com.flixclusive.data.database.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserAuthRepositoryImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val appDispatchers: AppDispatchers,
) : UserAuthRepository {
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

            return@withContext user != null
        }
    }

    override suspend fun signIn(user: User) {
        withContext(appDispatchers.io) {
            userSessionDataStore.saveCurrentUserId(user.id, user.legacyId)
        }
    }

    override suspend fun signOut() {
        withContext(appDispatchers.io) {
            userSessionDataStore.clearCurrentUser()
        }
    }
}
