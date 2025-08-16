package com.flixclusive.feature.mobile.user.edit

import com.flixclusive.core.database.entity.user.User
import com.flixclusive.domain.database.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal class FakeUserRepository(
    users: List<User>,
    private val dispatcher: CoroutineDispatcher
) : UserRepository {
    private val _users = MutableStateFlow(users)

    override fun observeUsers(): Flow<List<User>> {
        return _users.asStateFlow()
    }

    override suspend fun getUser(id: Int): User? {
        return withContext(dispatcher) {
            _users.value.find { it.id == id }
        }
    }

    override fun observeUser(id: Int): Flow<User?> {
        return _users.asStateFlow().map { users ->
            users.find { it.id == id }
        }
    }

    override suspend fun addUser(user: User) {
        withContext(dispatcher) {
            _users.update { currentUsers ->
                // If user with same ID exists, replace it, otherwise add new
                val updatedUsers = currentUsers.toMutableList()
                val existingIndex = updatedUsers.indexOfFirst { it.id == user.id }

                if (existingIndex != -1) {
                    updatedUsers[existingIndex] = user
                } else {
                    updatedUsers.add(user)
                }

                updatedUsers.toList()
            }
        }
    }

    override suspend fun deleteUser(id: Int) {
        withContext(dispatcher) {
            _users.update { currentUsers ->
                currentUsers.filterNot { it.id == id }
            }
        }
    }
}
