package com.flixclusive.data.database.repository

import com.flixclusive.core.database.entity.user.User

interface UserAuthRepository {
    suspend fun hasOldSession(): Boolean

    suspend fun restoreSession()

    suspend fun signIn(user: User)

    suspend fun signOut()
}
