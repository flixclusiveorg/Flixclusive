package com.flixclusive.data.database.session.impl

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.testing.database.DatabaseTestDefaults
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.data.database.datasource.impl.LocalUserDataSource
import com.flixclusive.data.database.repository.impl.UserRepositoryImpl
import com.flixclusive.data.database.session.fake.FakeUserSessionDataStore
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
class UserSessionManagerImplTest {
    private lateinit var database: AppDatabase
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var userSessionDataStore: UserSessionDataStore
    private lateinit var fakeUserSessionDataStore: FakeUserSessionDataStore
    private lateinit var userSessionManager: UserSessionManagerImpl
    private lateinit var appDispatchers: AppDispatchers

    private val testDispatcher = StandardTestDispatcher()
    private val testUser = DatabaseTestDefaults.getUser()

    @Before
    fun setUp() {
        database = DatabaseTestDefaults.createDatabase(
            context = ApplicationProvider.getApplicationContext(),
        )
        val userDataSource = LocalUserDataSource(database.userDao())
        userRepository = UserRepositoryImpl(userDataSource)
        fakeUserSessionDataStore = FakeUserSessionDataStore()
        userSessionDataStore = fakeUserSessionDataStore
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        userSessionManager = UserSessionManagerImpl(
            userRepository = userRepository,
            userSessionDataStore = userSessionDataStore,
            appDispatchers = appDispatchers,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun shouldObserveCurrentUser() =
        runTest(testDispatcher) {
            // Insert user into database
            userRepository.addUser(testUser)
            userSessionManager.signIn(testUser)

            userSessionManager.currentUser.test {
                skipItems(1) // Skip initial null item

                expectThat(awaitItem()).isNotNull().and {
                    get { id }.isEqualTo(testUser.id)
                    get { name }.isEqualTo(testUser.name)
                }
            }
        }

    @Test
    fun shouldObserveNullWhenNoCurrentUserId() =
        runTest(testDispatcher) {
            userSessionManager.currentUser.test {
                val result = awaitItem()
                expectThat(result).isNull()
            }
        }

    @Test
    fun shouldSignInUser() =
        runTest(testDispatcher) {
            userRepository.addUser(testUser)

            userSessionManager.currentUser.test {
                expectThat(awaitItem()).isNull() // Ensure no user is set initially

                userSessionManager.signIn(testUser)

                expectThat(awaitItem()).isNotNull().and {
                    get { id }.isEqualTo(testUser.id)
                    get { name }.isEqualTo(testUser.name)
                }
            }
        }

    @Test
    fun shouldSignOutUser() =
        runTest(testDispatcher) {
            userRepository.addUser(testUser)
            userSessionManager.currentUser.test {
                skipItems(1) // Skip initial null item

                userSessionManager.signIn(testUser)
                expectThat(awaitItem()).isNotNull().and {
                    get { id }.isEqualTo(testUser.id)
                    get { name }.isEqualTo(testUser.name)
                }

                userSessionManager.signOut()
                expectThat(awaitItem()).isNull()
            }
        }

    @Test
    fun shouldReturnTrueForValidOldSession() =
        runTest(testDispatcher) {
            // Insert user into database
            userRepository.addUser(testUser)

            val futureTime = System.currentTimeMillis() + 3600000L // 1 hour in future

            val tempUserSessionManager = UserSessionManagerImpl(
                userRepository = userRepository,
                appDispatchers = appDispatchers,
                userSessionDataStore = FakeUserSessionDataStore(
                    testUserId = testUser.id,
                    testSessionTimeout = futureTime,
                ),
            )

            val hasOldSession = tempUserSessionManager.hasOldSession()
            expectThat(hasOldSession).isTrue()
        }

    @Test
    fun shouldReturnFalseWhenNoSavedUserId() =
        runTest(testDispatcher) {
            val hasOldSession = userSessionManager.hasOldSession()
            expectThat(hasOldSession).isFalse()
        }

    @Test
    fun shouldReturnFalseWhenSessionIsExpired() =
        runTest(testDispatcher) {
            // Insert user into database
            userRepository.addUser(testUser)

            val pastTime = System.currentTimeMillis() - 3600000L // 1 hour in past

            val tempUserSessionManager = UserSessionManagerImpl(
                userRepository = userRepository,
                appDispatchers = appDispatchers,
                userSessionDataStore = FakeUserSessionDataStore(
                    testUserId = testUser.id,
                    testSessionTimeout = pastTime,
                ),
            )

            val hasOldSession = tempUserSessionManager.hasOldSession()
            expectThat(hasOldSession).isFalse()
        }

    @Test
    fun shouldReturnFalseWhenUserDoesNotExist() =
        runTest(testDispatcher) {
            val futureTime = System.currentTimeMillis() + 3600000L

            val tempUserSessionManager = UserSessionManagerImpl(
                userRepository = userRepository,
                appDispatchers = appDispatchers,
                userSessionDataStore = FakeUserSessionDataStore(
                    testUserId = 999,
                    testSessionTimeout = futureTime,
                ),
            )

            val hasOldSession = tempUserSessionManager.hasOldSession()
            expectThat(hasOldSession).isFalse()
        }

    @Test
    fun shouldRestoreSessionSuccessfully() =
        runTest(testDispatcher) {
            // Insert user into database
            userRepository.addUser(testUser)

            val futureTime = System.currentTimeMillis() + 3600000L

            val tempUserSessionManager = UserSessionManagerImpl(
                userRepository = userRepository,
                appDispatchers = appDispatchers,
                userSessionDataStore = FakeUserSessionDataStore(
                    testUserId = testUser.id,
                    testSessionTimeout = futureTime,
                ),
            )

            tempUserSessionManager.currentUser.test {
                expectThat(awaitItem()).isNull() // Ensure no user is set initially

                tempUserSessionManager.restoreSession()

                expectThat(awaitItem()).isNotNull().and {
                    get { id }.isEqualTo(testUser.id)
                    get { name }.isEqualTo(testUser.name)
                }
            }
        }
}
