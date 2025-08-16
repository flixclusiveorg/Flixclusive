package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.UserDao
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.data.database.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class UserRepositoryImpl
    @Inject
    constructor(
        private val userDao: UserDao,
        private val appDispatchers: AppDispatchers
    ) : UserRepository {
        override fun observeUsers(): Flow<List<User>> {
            return userDao.getAllAsFlow()
        }

        override suspend fun getUser(id: Int): User? {
            return withContext(appDispatchers.io) {
                userDao.get(id)
            }
        }

        override fun observeUser(id: Int): Flow<User?> {
            return userDao.getAsFlow(id)
        }

        override suspend fun addUser(user: User): Long {
            return withContext(appDispatchers.io) {
                userDao.insert(user)
            }
        }

        override suspend fun updateUser(user: User) {
            return withContext(appDispatchers.io) {
                userDao.update(user)
            }
        }

        override suspend fun deleteUser(id: Int) {
            return withContext(appDispatchers.io) {
                userDao.delete(id)
            }
        }
    }
