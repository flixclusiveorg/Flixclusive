package com.flixclusive.data.user

import com.flixclusive.data.user.local.UserDataSource
import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeDataSource : UserDataSource {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    private val users: Flow<List<User>> = _users

    override fun observeUsers(): Flow<List<User>> = users

    override fun observeUser(id: Int): Flow<User?> = MutableStateFlow(
        _users.value.find { it.id == id }
    )

    override suspend fun addUser(user: User) {
        _users.update { currentUsers ->
            // Replace existing user or add new user
            val updatedUsers = currentUsers.toMutableList()
            val existingIndex = updatedUsers.indexOfFirst { it.id == user.id }
            if (existingIndex != -1) {
                updatedUsers[existingIndex] = user
            } else {
                updatedUsers.add(user)
            }
            updatedUsers
        }
    }

    override suspend fun deleteUser(id: Int) {
        _users.update { currentUsers ->
            currentUsers.filter { it.id != id }
        }
    }

    override suspend fun getUser(id: Int): User? {
        return _users.value.find { it.id == id }
    }

    // Helper method to set initial state for testing
    fun setUsers(users: List<User>) {
        _users.value = users
    }
}
