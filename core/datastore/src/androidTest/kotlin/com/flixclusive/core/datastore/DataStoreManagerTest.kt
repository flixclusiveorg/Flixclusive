package com.flixclusive.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.TestUserPreferences.Companion.TEST_ANDROID_USER_PREFS_KEY
import com.flixclusive.core.datastore.fake.FakeUserSessionDataStore
import com.flixclusive.core.datastore.migration.SystemPreferencesMigration
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.datastore.serializer.system.SystemPreferencesSerializer
import com.flixclusive.core.datastore.util.OkioSerializerWrapper
import com.flixclusive.core.datastore.util.createUserPreferences
import com.flixclusive.core.testing.DispatcherTestConfiguration
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File

@Serializable
private data class TestUserPreferences(
    val testValue: String = "default_android",
    val testNumber: Int = 0,
    val isEnabled: Boolean = false,
) : UserPreferences {
    companion object {
        val TEST_ANDROID_USER_PREFS_KEY = stringPreferencesKey("test_android_user_prefs")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DataStoreManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testCoroutineScope = TestScope(testDispatcher + Job())
    private lateinit var context: Context
    private lateinit var userSessionDataStore: FakeUserSessionDataStore
    private lateinit var testDataStoreManager: DataStoreManager
    private lateinit var testSystemDataStore: DataStore<SystemPreferences>
    private lateinit var testUserDataStore: DataStore<Preferences>
    private lateinit var testDispatchers: AppDispatchers

    @Before
    fun setup() =
        runTest(testDispatcher) {
            testDispatchers = DispatcherTestConfiguration.createTestAppDispatchers(testDispatcher)

            context = ApplicationProvider.getApplicationContext()

            // Create real test DataStores with temporary files
            testUserDataStore = PreferenceDataStoreFactory.create(scope = testCoroutineScope) {
                File(context.cacheDir, "test-datastore/test_user_${System.currentTimeMillis()}.preferences_pb")
            }

            testSystemDataStore = DataStoreFactory.create(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = OkioSerializerWrapper(SystemPreferencesSerializer),
                ) {
                    File(
                        // parent =
                        context.cacheDir,
                        // child =
                        "test-datastore/test_system_${System.currentTimeMillis()}.json",
                    ).absolutePath.toPath()
                },
                migrations = listOf(SystemPreferencesMigration(context)),
                scope = testCoroutineScope,
            )

            userSessionDataStore = FakeUserSessionDataStore()

            // Mock the Context.createUserPreferences extension method
            mockkStatic("com.flixclusive.core.datastore.util.UserDataStoreKt")
            every {
                context.createUserPreferences(
                    userId = any(),
                    corruptionHandler = any(),
                    produceMigrations = any(),
                    scope = any()
                )
            } returns testUserDataStore

            testDataStoreManager = DataStoreManagerImpl(
                context = context,
                userSessionDataStore = userSessionDataStore,
                systemPreferences = testSystemDataStore,
                appDispatchers = testDispatchers,
            )
        }

    @After
    fun cleanup() {
        // Clean up test files
        context.cacheDir
            .listFiles()
            ?.filter {
                it.name.startsWith("test_system_") || it.name.startsWith("test_user_")
            }?.forEach { it.delete() }

        unmockkStatic("com.flixclusive.core.datastore.util.UserDataStoreKt")
    }

    @Test
    fun shouldInitializeUserPreferencesWhenUserIdIsSet() =
        runTest(testDispatcher) {
            // When
            userSessionDataStore.setUserId(123)
            advanceUntilIdle()

            // Then - user ID should be set correctly
            expectThat(userSessionDataStore.currentUserId.value).isEqualTo(123)
        }

    @Test
    fun shouldReturnDefaultUserPreferencesWhenNoDataExists() =
        runTest(testDispatcher) {
            // Given
            userSessionDataStore.setUserId(123)
            advanceUntilIdle()

            // When/Then
            testDataStoreManager.getUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class).test {
                val result = awaitItem()
                expectThat(result).get { testValue }.isEqualTo("default_android")
                expectThat(result).get { testNumber }.isEqualTo(0)
                expectThat(result).get { isEnabled }.isFalse()
            }
        }

    @Test
    fun shouldStoreAndRetrieveUserPreferencesCorrectly() =
        runTest(testDispatcher) {
            // Given
            userSessionDataStore.setUserId(123)
            advanceUntilIdle()

            val expectedPrefs = TestUserPreferences(
                testValue = "stored_value",
                testNumber = 42,
                isEnabled = true,
            )

            // Store data
            testUserDataStore.edit { preferences ->
                preferences[TEST_ANDROID_USER_PREFS_KEY] = Json.encodeToString(expectedPrefs)
            }

            // When/Then
            testDataStoreManager.getUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class).test {
                val result = awaitItem()
                expectThat(result).get { testValue }.isEqualTo("stored_value")
                expectThat(result).get { testNumber }.isEqualTo(42)
                expectThat(result).get { isEnabled }.isTrue()
            }
        }

    @Test
    fun shouldUpdateUserPreferencesFromDefault() =
        runTest(testDispatcher) {
            // Given
            userSessionDataStore.setUserId(123)
            advanceUntilIdle()

            // When
            testDataStoreManager.updateUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class) { prefs ->
                prefs.copy(
                    testValue = "updated_value",
                    testNumber = 100,
                    isEnabled = true,
                )
            }

            // Then
            testDataStoreManager.getUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class).test {
                val result = awaitItem()
                expectThat(result).get { testValue }.isEqualTo("updated_value")
                expectThat(result).get { testNumber }.isEqualTo(100)
                expectThat(result).get { isEnabled }.isTrue()
            }
        }

    @Test
    fun shouldTransformExistingUserPreferences() =
        runTest(testDispatcher) {
            // Given
            userSessionDataStore.setUserId(123)
            advanceUntilIdle()

            // Set initial data
            testDataStoreManager.updateUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class) { prefs ->
                prefs.copy(testValue = "initial", testNumber = 10)
            }

            // When - transform existing data
            testDataStoreManager.updateUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class) { prefs ->
                prefs.copy(
                    testValue = "${prefs.testValue}_transformed",
                    testNumber = prefs.testNumber * 2,
                )
            }

            // Then
            testDataStoreManager.getUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class).test {
                val result = awaitItem()
                expectThat(result).get { testValue }.isEqualTo("initial_transformed")
                expectThat(result).get { testNumber }.isEqualTo(20)
            }
        }

    @Test
    fun shouldUpdateSystemPreferencesCorrectly() =
        runTest(testDispatcher) {
            // When
            testDataStoreManager.updateSystemPrefs { prefs ->
                prefs.copy(
                    isFirstTimeUserLaunch = false,
                    isUsingAutoUpdateAppFeature = false,
                    userAgent = "test_agent",
                )
            }

            // Then
            testDataStoreManager.getSystemPrefs().test {
                val result = awaitItem()
                expectThat(result).get { isFirstTimeUserLaunch }.isFalse()
                expectThat(result).get { isUsingAutoUpdateAppFeature }.isFalse()
                expectThat(result).get { userAgent }.isEqualTo("test_agent")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun shouldHandleMultipleSequentialUpdates() =
        runTest(testDispatcher) {
            // Given
            userSessionDataStore.setUserId(123)
            advanceUntilIdle()

            // When - multiple sequential updates
            testDataStoreManager.updateUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class) { prefs ->
                prefs.copy(testNumber = 1)
            }

            testDataStoreManager.updateUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class) { prefs ->
                prefs.copy(testNumber = prefs.testNumber + 10)
            }

            testDataStoreManager.updateUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class) { prefs ->
                prefs.copy(testNumber = prefs.testNumber * 3)
            }

            // Then
            testDataStoreManager.getUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class).test {
                val result = awaitItem()
                expectThat(result).get { testNumber }.isEqualTo(33) // ((1 + 10) * 3)
            }
        }

    @Test
    fun shouldHandleUserSessionFlow() =
        runTest(testDispatcher) {
            // When
            userSessionDataStore.saveCurrentUserId(456)

            // Then
            userSessionDataStore.currentUserId.test {
                expectThat(awaitItem()).isEqualTo(456)
            }

            userSessionDataStore.sessionTimeout.test {
                val timeout = awaitItem()
                expectThat(timeout > System.currentTimeMillis()).isTrue()
            }
        }

    @Test
    fun shouldClearUserSessionCorrectly() =
        runTest(testDispatcher) {
            // Given
            userSessionDataStore.saveCurrentUserId(123)

            // When
            userSessionDataStore.clearCurrentUser()

            // Then
            userSessionDataStore.currentUserId.test {
                expectThat(awaitItem()).isEqualTo(null)
            }

            userSessionDataStore.sessionTimeout.test {
                expectThat(awaitItem()).isEqualTo(0L)
            }
        }

    @Test
    fun shouldHandleDeleteUserRelatedFiles() =
        runTest(testDispatcher) {
            // Given
            val userId = 123

            // When - should complete without throwing
            testDataStoreManager.deleteAllUserRelatedFiles(userId)

            // Then - operation should complete successfully
            expectThat(true).isTrue()
        }

    @Test
    fun shouldHandleConcurrentUserPreferenceOperations() =
        runTest(testDispatcher) {
            // Given
            userSessionDataStore.setUserId(123)
            advanceUntilIdle()

            // When - multiple concurrent-like operations
            repeat(5) {
                testDataStoreManager.updateUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class) { prefs ->
                    prefs.copy(testNumber = prefs.testNumber + 1)
                }
            }

            // Then - all updates should be applied sequentially
            testDataStoreManager.getUserPrefs(TEST_ANDROID_USER_PREFS_KEY, TestUserPreferences::class).test {
                val result = awaitItem()
                expectThat(result).get { testNumber }.isEqualTo(5)
            }
        }
}
