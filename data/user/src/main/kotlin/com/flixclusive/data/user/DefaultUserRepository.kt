package com.flixclusive.data.user

import com.flixclusive.core.database.dao.UserDao
import com.flixclusive.core.util.coroutines.AppDispatchers.Companion.withIOContext
import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class DefaultUserRepository @Inject constructor(
    private val userDao: UserDao
) : UserRepository {
    override fun getAllUsers(): Flow<List<User>> = userDao.getAllItemsInFlow()

    override suspend fun getUserById(id: Int): User? {
        return withIOContext {
            userDao.getUserById(id)
        }
    }

    override fun getUserByIdFlow(id: Int): Flow<User?> = userDao.getUserByIdInFlow(id)

    override suspend fun insert(user: User) {
        withIOContext {
            userDao.insert(user)
        }
    }

    override suspend fun deleteById(id: Int) {
        withIOContext {
            userDao.deleteById(id)
        }
    }
}