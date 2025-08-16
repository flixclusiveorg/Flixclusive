package com.flixclusive.data.database.repository.impl

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@RunWith(AndroidJUnit4::class)
class UserRepositoryImplTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: UserRepositoryImpl
    private lateinit var appDispatchers: AppDispatchers

    private val testDispatcher = StandardTestDispatcher()
    private val testUser = DatabaseTestDefaults.getUser()

    @Before
    fun setUp() {
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        database = DatabaseTestDefaults.createDatabase(
            context = ApplicationProvider.getApplicationContext(),
        )

        repository = UserRepositoryImpl(
            userDao = database.userDao(),
            appDispatchers = appDispatchers,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun shouldAddAndRetrieveUser() =
        runTest(testDispatcher) {
            val userId = repository.addUser(testUser)

            val retrievedUser = repository.getUser(userId.toInt())
            expectThat(retrievedUser).isNotNull().and {
                get { name }.isEqualTo(testUser.name)
                get { image }.isEqualTo(testUser.image)
            }
        }

    @Test
    fun shouldObserveUsers() =
        runTest(testDispatcher) {
            val user1 = testUser.copy(id = 1, name = "User 1")
            val user2 = testUser.copy(id = 2, name = "User 2")

            repository.addUser(user1)
            repository.addUser(user2)

            repository.observeUsers().test {
                val result = awaitItem()
                expectThat(result).hasSize(2)
            }
        }

    @Test
    fun shouldObserveSpecificUser() =
        runTest(testDispatcher) {
            val userId = repository.addUser(testUser)

            repository.observeUser(userId.toInt()).test {
                val result = awaitItem()
                expectThat(result).isNotNull().and {
                    get { name }.isEqualTo(testUser.name)
                    get { image }.isEqualTo(testUser.image)
                }
            }
        }

    @Test
    fun shouldUpdateUser() =
        runTest(testDispatcher) {
            val userId = repository.addUser(testUser)

            val updatedUser = testUser.copy(
                id = userId.toInt(),
                name = "Updated User",
                image = 2,
            )

            repository.updateUser(updatedUser)

            val retrievedUser = repository.getUser(userId.toInt())
            expectThat(retrievedUser).isNotNull().and {
                get { name }.isEqualTo("Updated User")
                get { image }.isEqualTo(2)
            }
        }

    @Test
    fun shouldDeleteUser() =
        runTest(testDispatcher) {
            val userId = repository.addUser(testUser)

            repository.deleteUser(userId.toInt())

            val retrievedUser = repository.getUser(userId.toInt())
            expectThat(retrievedUser).isNull()
        }

    @Test
    fun shouldReturnNullForNonExistentUser() =
        runTest(testDispatcher) {
            val retrievedUser = repository.getUser(999)
            expectThat(retrievedUser).isNull()
        }

    @Test
    fun shouldObserveNullForNonExistentUser() =
        runTest(testDispatcher) {
            repository.observeUser(999).test {
                val result = awaitItem()
                expectThat(result).isNull()
            }
        }
}
