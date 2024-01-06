package com.flixclusive.data.user

import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getAllUsers(): Flow<List<User>>

    suspend fun getUserById(id: Int): User?

    fun getUserByIdFlow(id: Int): Flow<User?>

    suspend fun insert(user: User)

    suspend fun deleteById(id: Int)
}