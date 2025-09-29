package com.flixclusive.feature.splashScreen

import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.system.SystemPreferences
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.data.app.updates.model.AppUpdateInfo
import com.flixclusive.data.app.updates.repository.AppUpdatesRepository
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.session.UserSessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

class SplashScreenViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appDispatchers: AppDispatchers

    private val userSessionManager: UserSessionManager = mockk()
    private val userRepository: UserRepository = mockk()
    private val appUpdatesRepository: AppUpdatesRepository = mockk()
    private val dataStoreManager: DataStoreManager = mockk()

    private lateinit var viewModel: SplashScreenViewModel

    private val testSystemPreferences = SystemPreferences()
    private val testUser = User(
        id = 123,
        name = "Test User",
        image = 1,
        pin = null,
        pinHint = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        every { userSessionManager.currentUser } returns MutableStateFlow(null)
        every { userRepository.observeUsers() } returns flowOf(emptyList())
        every { dataStoreManager.getSystemPrefs() } returns flowOf(testSystemPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be loading`() =
        runTest(testDispatcher) {
            coEvery { appUpdatesRepository.getLatestUpdate() } returns Result.success(null)

            viewModel = createViewModel()

            viewModel.uiState.test {
                val initialState = awaitItem()
                expectThat(initialState.isLoading).isTrue()
                expectThat(initialState.error).isNull()
                expectThat(initialState.newAppUpdateInfo).isNull()
            }
        }

    @Test
    fun `should emit success state when update check succeeds with update info`() =
        runTest(testDispatcher) {
            val appUpdateInfo = AppUpdateInfo(
                versionName = "1.0.1",
                changelogs = "Bug fixes and improvements",
                updateUrl = "https://example.com/update.apk",
            )
            coEvery { appUpdatesRepository.getLatestUpdate() } returns Result.success(appUpdateInfo)

            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.uiState.test {
                val finalState = awaitItem()
                expectThat(finalState.isLoading).isFalse()
                expectThat(finalState.error).isNull()
                expectThat(finalState.newAppUpdateInfo).isEqualTo(appUpdateInfo)
            }
        }

    @Test
    fun `should emit success state when update check succeeds with null`() =
        runTest(testDispatcher) {
            coEvery { appUpdatesRepository.getLatestUpdate() } returns Result.success(null)

            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.uiState.test {
                val finalState = awaitItem()
                expectThat(finalState.isLoading).isFalse()
                expectThat(finalState.error).isNull()
                expectThat(finalState.newAppUpdateInfo).isNull()
            }
        }

    @Test
    fun `should emit error state when update check fails`() =
        runTest(testDispatcher) {
            val testException = RuntimeException("Network error")
            coEvery { appUpdatesRepository.getLatestUpdate() } returns Result.failure(testException)

            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.uiState.test {
                val finalState = awaitItem()
                expectThat(finalState.isLoading).isFalse()
                expectThat(finalState.error).isEqualTo(testException)
                expectThat(finalState.newAppUpdateInfo).isNull()
            }
        }

    @Test
    fun `noUsersFound should be false when users exist`() =
        runTest(testDispatcher) {
            val users = listOf(testUser)
            every { userRepository.observeUsers() } returns flowOf(users)
            coEvery { appUpdatesRepository.getLatestUpdate() } returns Result.success(null)

            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.noUsersFound.test {
                expectThat(awaitItem()).isFalse()
            }
        }

    @Test
    fun `userLoggedIn should reflect current user state`() =
        runTest(testDispatcher) {
            every { userSessionManager.currentUser } returns MutableStateFlow(testUser)
            coEvery { appUpdatesRepository.getLatestUpdate() } returns Result.success(null)

            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.userLoggedIn.test {
                expectThat(awaitItem()).isEqualTo(testUser)
            }
        }

    @Test
    fun `updateSettings should update system preferences`() =
        runTest(testDispatcher) {
            coEvery { appUpdatesRepository.getLatestUpdate() } returns Result.success(null)
            coEvery { dataStoreManager.updateSystemPrefs(any()) } returns Unit

            viewModel = createViewModel()

            val transform: suspend (SystemPreferences) -> SystemPreferences = { prefs ->
                prefs.copy()
            }

            viewModel.updateSettings(transform)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { dataStoreManager.updateSystemPrefs(transform) }
        }

    @Test
    fun `updateSettings should not execute if already active`() =
        runTest(testDispatcher) {
            coEvery { appUpdatesRepository.getLatestUpdate() } returns Result.success(null)
            coEvery { dataStoreManager.updateSystemPrefs(any()) } returns Unit

            viewModel = createViewModel()

            val transform: suspend (SystemPreferences) -> SystemPreferences = { prefs ->
                prefs.copy()
            }

            // Start first job
            viewModel.updateSettings(transform)

            // Try to start second job before first completes
            viewModel.updateSettings(transform)

            testDispatcher.scheduler.advanceUntilIdle()

            // Verify updateSystemPrefs was only called once
            coVerify(exactly = 1) { dataStoreManager.updateSystemPrefs(any()) }
        }

    @Test
    fun `should handle multiple state transitions correctly`() =
        runTest(testDispatcher) {
            val appUpdateInfo = AppUpdateInfo(
                versionName = "2.0.0",
                changelogs = "Major update with new features",
                updateUrl = "https://example.com/v2.apk",
            )
            coEvery { appUpdatesRepository.getLatestUpdate() } returns Result.success(appUpdateInfo)

            viewModel = createViewModel()

            viewModel.uiState.test {
                // Initial loading state
                val loadingState = awaitItem()
                expectThat(loadingState.isLoading).isTrue()
                expectThat(loadingState.error).isNull()
                expectThat(loadingState.newAppUpdateInfo).isNull()

                testDispatcher.scheduler.advanceUntilIdle()

                // Final success state
                val successState = awaitItem()
                expectThat(successState.isLoading).isFalse()
                expectThat(successState.error).isNull()
                expectThat(successState.newAppUpdateInfo).isEqualTo(appUpdateInfo)
            }
        }

    @Test
    fun `should initialize systemPreferences flow with correct sharing strategy`() =
        runTest(testDispatcher) {
            coEvery { appUpdatesRepository.getLatestUpdate() } returns Result.success(null)

            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify the flow emits the expected value immediately due to SharingStarted.Eagerly
            viewModel.systemPreferences.test {
                expectThat(awaitItem()).isEqualTo(testSystemPreferences)
            }
        }

    @Test
    fun `should initialize noUsersFound flow with correct sharing strategy`() =
        runTest(testDispatcher) {
            coEvery { appUpdatesRepository.getLatestUpdate() } returns Result.success(null)

            viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify the flow emits the expected value immediately due to SharingStarted.Eagerly
            viewModel.noUsersFound.test {
                expectThat(awaitItem()).isTrue() // Default empty list should result in true
            }
        }

    @Test
    fun `SplashScreenUiState data class should have correct default values`() {
        val defaultState = SplashScreenUiState()

        expectThat(defaultState.isLoading).isFalse()
        expectThat(defaultState.error).isNull()
        expectThat(defaultState.newAppUpdateInfo).isNull()
    }

    @Test
    fun `SplashScreenUiState should allow custom values`() {
        val testException = RuntimeException("Test error")
        val appUpdateInfo = AppUpdateInfo(
            versionName = "1.0.0",
            changelogs = "Initial release",
            updateUrl = "https://example.com/app.apk",
        )

        val customState = SplashScreenUiState(
            isLoading = true,
            error = testException,
            newAppUpdateInfo = appUpdateInfo,
        )

        expectThat(customState.isLoading).isTrue()
        expectThat(customState.error).isEqualTo(testException)
        expectThat(customState.newAppUpdateInfo).isEqualTo(appUpdateInfo)
    }

    private fun createViewModel(): SplashScreenViewModel {
        return SplashScreenViewModel(
            userSessionManager = userSessionManager,
            userRepository = userRepository,
            appUpdatesRepository = appUpdatesRepository,
            dataStoreManager = dataStoreManager,
            appDispatchers = appDispatchers,
        )
    }
}
