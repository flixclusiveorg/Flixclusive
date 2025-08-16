package com.flixclusive.data.database.session

import com.flixclusive.core.database.entity.user.User
import kotlinx.coroutines.flow.StateFlow

interface UserSessionManager {
    val currentUser: StateFlow<User?>

    suspend fun hasOldSession(): Boolean

    suspend fun restoreSession()

    suspend fun signIn(user: User)

    suspend fun signOut()
}
