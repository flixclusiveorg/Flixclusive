package com.flixclusive.domain.user

import com.flixclusive.model.database.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DefaultUserSessionManagerTest {
    private lateinit var userSessionManager: UserSessionManager
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var fakeUserSessionDataStore: FakeUserSessionDataStore

    @Before
    fun setup() {
        fakeUserRepository =
            FakeUserRepository(
                users =
                    listOf(
                        User(id = 1, name = "Test User"),
                        User(id = 2, name = "Another User"),
                    ),
                dispatcher = UnconfinedTestDispatcher(),
            )

        fakeUserSessionDataStore = FakeUserSessionDataStore()

        userSessionManager =
            DefaultUserSessionManager(
                userRepository = fakeUserRepository,
                dataStoreManager = fakeUserSessionDataStore,
            )
    }

    @Test
    fun `restoreSession should restore user if session exists`() =
        runTest {
            val fakeUser = User(id = 1, name = "Test User")

            userSessionManager.signIn(fakeUser) // Simulate sign-in
            userSessionManager.hasOldSession() // Restore session

            val restoredUser = userSessionManager.currentUser.first()
            assertEquals(fakeUser, restoredUser)
        }

    @Test
    fun `signIn should save user and update current user`() =
        runTest {
            val fakeUser = User(id = 1, name = "Test User")

            userSessionManager.signIn(fakeUser) // Simulate sign-in

            val currentUser = userSessionManager.currentUser.first()
            assertEquals(fakeUser, currentUser)
        }

    @Test
    fun `signOut should clear the current user`() =
        runTest {
            val fakeUser = User(id = 1, name = "Test User")

            userSessionManager.signIn(fakeUser) // Simulate sign-in
            userSessionManager.signOut() // Sign out

            val currentUser = userSessionManager.currentUser.first()
            assertNull(currentUser)
        }
}
