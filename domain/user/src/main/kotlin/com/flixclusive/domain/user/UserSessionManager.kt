package com.flixclusive.domain.user

import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.StateFlow

interface UserSessionManager {
    val currentUser: StateFlow<User?>
    suspend fun restoreSession()
    suspend fun signIn(user: User)
    suspend fun signOut()
}