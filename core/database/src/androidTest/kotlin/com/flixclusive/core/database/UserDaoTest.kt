package com.flixclusive.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.database.dao.UserDao
import com.flixclusive.core.database.entity.user.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private lateinit var userDao: UserDao
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).build()
        userDao = db.userDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun shouldInsertAndRetrieveUser() =
        runTest {
            val user = User(
                id = 1,
                name = "testuser",
                image = 1,
            )

            userDao.insert(user)

            val retrievedUser = userDao.get(1)
            expectThat(retrievedUser).isNotNull()
            expectThat(retrievedUser!!.id).isEqualTo(1)
            expectThat(retrievedUser.name).isEqualTo("testuser")
            expectThat(retrievedUser.image).isEqualTo(1)
        }

    @Test
    fun shouldReturnNullForNonexistentUser() =
        runTest {
            val result = userDao.get(999)
            expectThat(result).isNull()
        }

    @Test
    fun shouldGetAllUsers() =
        runTest {
            val user1 = User(id = 1, name = "user1", image = 1)
            val user2 = User(id = 2, name = "user2", image = 2)
            val user3 = User(id = 3, name = "user3", image = 3)

            userDao.insert(user1)
            userDao.insert(user2)
            userDao.insert(user3)

            val allUsers = userDao.getAllAsFlow().first()
            expectThat(allUsers).hasSize(3)
            expectThat(allUsers.map { it.id }).isEqualTo(listOf(1, 2, 3))
        }

    @Test
    fun shouldReturnEmptyListWhenNoUsers() =
        runTest {
            val allUsers = userDao.getAllAsFlow().first()
            expectThat(allUsers).isEmpty()
        }

    @Test
    fun shouldUpdateUser() =
        runTest {
            val user = User(id = 1, name = "originalname", image = 1)
            userDao.insert(user)

            val updatedUser = user.copy(
                name = "updatedname",
                image = 2,
            )
            userDao.update(updatedUser)

            val retrievedUser = userDao.get(1)
            expectThat(retrievedUser).isNotNull()
            expectThat(retrievedUser!!.name).isEqualTo("updatedname")
            expectThat(retrievedUser.image).isEqualTo(2)
        }

    @Test
    fun shouldDeleteUser() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            userDao.insert(user)

            userDao.delete(1)

            val retrievedUser = userDao.get(1)
            expectThat(retrievedUser).isNull()
        }

    @Test
    fun shouldGetUserAsFlow() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1)
            userDao.insert(user)

            val userFlow = userDao.getAsFlow(1).first()
            expectThat(userFlow).isNotNull()
            expectThat(userFlow!!.id).isEqualTo(1)
            expectThat(userFlow.name).isEqualTo("testuser")
        }

    @Test
    fun shouldHandleUserWithPin() =
        runTest {
            val user = User(id = 1, name = "testuser", image = 1, pin = "1234", pinHint = "test hint")
            userDao.insert(user)

            val retrievedUser = userDao.get(1)
            expectThat(retrievedUser).isNotNull()
            expectThat(retrievedUser!!.pin).isEqualTo("1234")
            expectThat(retrievedUser.pinHint).isEqualTo("test hint")
        }

    @Test
    fun shouldHandleMultipleUsersWithSameName() =
        runTest {
            val user1 = User(id = 1, name = "duplicate", image = 1)
            val user2 = User(id = 2, name = "duplicate", image = 2)

            userDao.insert(user1)
            userDao.insert(user2)

            val allUsers = userDao.getAllAsFlow().first()
            expectThat(allUsers).hasSize(2)
            expectThat(allUsers.map { it.name }).isEqualTo(listOf("duplicate", "duplicate"))
        }
}
