package com.flixclusive.feature.mobile.profiles

import android.content.Context
import androidx.core.app.NotificationCompat
import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.util.android.notify
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderApiRepository
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.InitializeProvidersUseCase
import com.flixclusive.domain.provider.usecase.manage.LoadProviderResult
import com.flixclusive.domain.provider.usecase.updater.ProviderUpdateResult
import com.flixclusive.domain.provider.usecase.updater.UpdateProviderUseCase
import com.flixclusive.model.provider.ProviderMetadata
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfilesViewModelTest {
    private lateinit var viewModel: UserProfilesViewModel
    private lateinit var context: Context
    private lateinit var userSessionManager: UserSessionManager
    private lateinit var initializeProviders: InitializeProvidersUseCase
    private lateinit var updateProvider: UpdateProviderUseCase
    private lateinit var providerRepository: ProviderRepository
    private lateinit var providerApiRepository: ProviderApiRepository
    private lateinit var appDispatchers: AppDispatchers
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var userRepository: UserRepository

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    private val currentUser = User(
        id = 1,
        name = "Current User",
        image = 0,
        pin = null,
        pinHint = null,
    )

    private val testUser1 = User(
        id = 2,
        name = "Test User 1",
        image = 1,
        pin = "1234",
        pinHint = "test hint",
    )

    private val testUser2 = User(
        id = 3,
        name = "Test User 2",
        image = 2,
        pin = null,
        pinHint = null,
    )

    private val testProvider1 = DummyDataForPreview.getDummyProviderMetadata(
        id = "test-provider-1",
        name = "Test Provider 1",
        versionName = "1.0.0",
    )

    private val testProvider2 = DummyDataForPreview.getDummyProviderMetadata(
        id = "test-provider-2",
        name = "Test Provider 2",
        versionName = "2.0.0",
    )

    private val testProviderPreferences = ProviderPreferences(
        isAutoUpdateEnabled = true,
        shouldWarnBeforeInstall = false,
        repositories = emptyList(),
        providers = emptyList(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        userSessionManager = mockk(relaxed = true)
        initializeProviders = mockk(relaxed = true)
        updateProvider = mockk(relaxed = true)
        providerRepository = mockk(relaxed = true)
        providerApiRepository = mockk(relaxed = true)
        dataStoreManager = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)

        mockkStatic("com.flixclusive.core.util.android.NotificationKt")

        appDispatchers = object : AppDispatchers {
            override val default: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher
            override val ioScope: CoroutineScope = CoroutineScope(testDispatcher)
            override val defaultScope: CoroutineScope = CoroutineScope(testDispatcher)
            override val mainScope: CoroutineScope = CoroutineScope(testDispatcher)
        }

        every { userSessionManager.currentUser } returns MutableStateFlow(currentUser).asStateFlow()
        every { userRepository.observeUsers() } returns flowOf(listOf(currentUser, testUser1, testUser2))
        every { dataStoreManager.getUserPrefs(UserPreferences.PROVIDER_PREFS_KEY, ProviderPreferences::class) } returns
            flowOf(testProviderPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = UserProfilesViewModel(
            context = context,
            userSessionManager = userSessionManager,
            initializeProviders = initializeProviders,
            updateProvider = updateProvider,
            providerRepository = providerRepository,
            providerApiRepository = providerApiRepository,
            appDispatchers = appDispatchers,
            dataStoreManager = dataStoreManager,
            userRepository = userRepository,
        )
    }

    @Test
    fun `initial state should be correct`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            expectThat(viewModel.uiState.value) {
                get { isLoggedIn }.isFalse()
                get { isLoading }.isFalse()
                get { focusedProfile }.isEqualTo(null)
                get { errors }.isEmpty()
            }
        }

    @Test
    fun `profiles should filter out current logged in user`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.profiles.test {
                val profiles = awaitItem()
                expectThat(profiles).hasSize(2)
                expectThat(profiles[0]).isEqualTo(testUser1)
                expectThat(profiles[1]).isEqualTo(testUser2)
            }
        }

    @Test
    fun `onUseProfile should sign out current user and sign in new user`() =
        runTest(testDispatcher) {
            every { initializeProviders() } returns flowOf()
            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            advanceUntilIdle()

            coVerify(exactly = 1) { userSessionManager.signOut() }
            coVerify(exactly = 1) { providerRepository.clearAll() }
            coVerify(exactly = 1) { providerApiRepository.clearAll() }
            coVerify(exactly = 1) { userSessionManager.signIn(testUser1) }
        }

    @Test
    fun `onUseProfile should prevent concurrent login operations`() =
        runTest(testDispatcher) {
            every { initializeProviders() } returns flowOf()
            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            viewModel.onUseProfile(testUser2)
            advanceUntilIdle()

            coVerify(exactly = 1) { userSessionManager.signIn(testUser1) }
            coVerify(exactly = 0) { userSessionManager.signIn(testUser2) }
        }

    @Test
    fun `loadProviders should handle successful provider initialization`() =
        runTest(testDispatcher) {
            val successResult1 = LoadProviderResult.Success(testProvider1)
            val successResult2 = LoadProviderResult.Success(testProvider2)

            every { initializeProviders() } returns flow {
                emit(successResult1)
                emit(successResult2)
            }
            coEvery { updateProvider(any<List<ProviderMetadata>>()) } returns ProviderUpdateResult(
                success = emptyList(),
                failed = emptyList(),
            )

            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            advanceUntilIdle()

            viewModel.uiState.test {
                val finalState = awaitItem()
                expectThat(finalState) {
                    get { isLoggedIn }.isTrue()
                    get { isLoading }.isFalse()
                    get { errors }.isEmpty()
                }
            }
        }

    @Test
    fun `loadProviders should handle failed provider initialization`() =
        runTest(testDispatcher) {
            val error = RuntimeException("Provider failed to load")

            every { initializeProviders() } returns flow {
                emit(
                    LoadProviderResult.Failure(
                        provider = testProvider1,
                        filePath = "/test/path",
                        error = error,
                    )
                )
            }
            coEvery { updateProvider(any<List<ProviderMetadata>>()) } returns ProviderUpdateResult(
                success = emptyList(),
                failed = emptyList(),
            )

            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            advanceUntilIdle()

            viewModel.uiState.test {
                val finalState = awaitItem()
                expectThat(finalState) {
                    get { isLoggedIn }.isTrue()
                    get { isLoading }.isFalse()
                    get { errors }.hasSize(1)
                    get { errors[testProvider1.id]?.throwable }.isEqualTo(error)
                }
            }
        }

    @Test
    fun `updateProviders should not run when auto update is disabled`() =
        runTest(testDispatcher) {
            val disabledPreferences = testProviderPreferences.copy(isAutoUpdateEnabled = false)
            every {
                dataStoreManager.getUserPrefs(
                    UserPreferences.PROVIDER_PREFS_KEY,
                    ProviderPreferences::class,
                )
            } returns
                flowOf(disabledPreferences)
            every { initializeProviders() } returns flow {
                emit(LoadProviderResult.Success(testProvider1))
            }

            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            advanceUntilIdle()

            coVerify(exactly = 0) { updateProvider(any<List<ProviderMetadata>>()) }
        }

    @Test
    fun `updateProviders should handle successful provider updates`() =
        runTest(testDispatcher) {
            val updatedProvider = testProvider1.copy(versionName = "1.1.0")
            val updateResult = ProviderUpdateResult(
                success = listOf(updatedProvider),
                failed = emptyList(),
            )

            every { context.notify(any(), any(), any(), any(), any()) } just runs
            every { initializeProviders() } returns flow {
                emit(LoadProviderResult.Failure(testProvider1, "/path", RuntimeException("Initial error")))
            }
            coEvery { updateProvider(any<List<ProviderMetadata>>()) } returns updateResult

            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            advanceUntilIdle()

            viewModel.uiState.test {
                val finalState = awaitItem()
                expectThat(finalState) {
                    get { errors }.isEmpty()
                }
            }
        }

    @Test
    fun `updateProviders should handle failed provider updates`() =
        runTest(testDispatcher) {
            val updateError = RuntimeException("Update failed")
            val updateResult = ProviderUpdateResult(
                success = emptyList(),
                failed = listOf(testProvider1 to updateError),
            )

            every { initializeProviders() } returns flow {
                emit(LoadProviderResult.Success(testProvider1))
            }
            coEvery { updateProvider(any<List<ProviderMetadata>>()) } returns updateResult

            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            advanceUntilIdle()

            viewModel.uiState.test {
                val finalState = awaitItem()
                expectThat(finalState) {
                    get { errors }.hasSize(1)
                    get { errors[testProvider1.id]?.throwable }.isEqualTo(updateError)
                }
            }
        }

    @Test
    fun `updateProviders should send notification for successful updates`() =
        runTest(testDispatcher) {
            val updatedProvider = testProvider1.copy(versionName = "1.1.0")
            val updateResult = ProviderUpdateResult(
                success = listOf(updatedProvider),
                failed = emptyList(),
            )

            every { context.notify(any(), any(), any(), any(), any()) } just runs
            every { initializeProviders() } returns flow {
                emit(LoadProviderResult.Success(testProvider1))
            }
            coEvery { updateProvider(any<List<ProviderMetadata>>()) } returns updateResult

            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            advanceUntilIdle()

            val notificationSlot = slot<NotificationCompat.Builder.() -> Unit>()
            verify { context.notify(any(), any(), any(), any(), capture(notificationSlot)) }
        }

    @Test
    fun `updateProviders should not send notification when no providers are updated`() =
        runTest(testDispatcher) {
            val updateResult = ProviderUpdateResult(
                success = emptyList(),
                failed = listOf(testProvider1 to RuntimeException("Update failed")),
            )

            every { initializeProviders() } returns flow {
                emit(LoadProviderResult.Success(testProvider1))
            }
            coEvery { updateProvider(any<List<ProviderMetadata>>()) } returns updateResult
            every { context.notify(any(), any(), any(), any(), any()) } just runs

            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            advanceUntilIdle()

            verify(exactly = 0) { context.notify(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `updateProviders should not run when providers list is empty`() =
        runTest(testDispatcher) {
            every { initializeProviders() } returns flowOf()

            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            advanceUntilIdle()

            coVerify(exactly = 0) { updateProvider(any<List<ProviderMetadata>>()) }
        }

    @Test
    fun `onHoverProfile should update focused profile`() =
        runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            viewModel.onHoverProfile(testUser1)

            expectThat(viewModel.uiState.value.focusedProfile).isEqualTo(testUser1)
        }

    @Test
    fun `onConsumeErrors should clear errors map`() =
        runTest(testDispatcher) {
            val error = RuntimeException("Test error")
            val failureResult = LoadProviderResult.Failure(
                provider = testProvider1,
                filePath = "/test/path",
                error = error,
            )

            every { initializeProviders() } returns flow {
                emit(failureResult)
            }
            coEvery { updateProvider(any<List<ProviderMetadata>>()) } returns ProviderUpdateResult(
                success = emptyList(),
                failed = emptyList(),
            )

            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            advanceUntilIdle()

            expectThat(viewModel.uiState.value.errors).hasSize(1)

            viewModel.onConsumeErrors()

            expectThat(viewModel.uiState.value.errors).isEmpty()
        }

    @Test
    fun `filterOutCurrentLoggedInUser should filter current user from profiles list`() =
        runTest(testDispatcher) {
            val allUsers = listOf(currentUser, testUser1, testUser2)
            every { userRepository.observeUsers() } returns flowOf(allUsers)

            createViewModel()
            advanceUntilIdle()

            viewModel.profiles.test {
                val filteredProfiles = awaitItem()
                expectThat(filteredProfiles).hasSize(2)
                expectThat(filteredProfiles.none { it.id == currentUser.id }).isTrue()
            }
        }

    @Test
    fun `concurrent loadProviders operations should be prevented`() =
        runTest(testDispatcher) {
            every { initializeProviders() } returns flowOf()
            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            viewModel.onUseProfile(testUser2)
            advanceUntilIdle()

            coVerify(exactly = 1) { userSessionManager.signIn(any()) }
        }

    @Test
    fun `mixed provider results should be handled correctly`() =
        runTest(testDispatcher) {
            val successResult = LoadProviderResult.Success(testProvider1)
            val failureResult = LoadProviderResult.Failure(
                provider = testProvider2,
                filePath = "/test/path",
                error = RuntimeException("Provider 2 failed"),
            )

            val error = RuntimeException("Update failed")

            every { context.notify(any(), any(), any(), any(), any()) } just runs
            every { initializeProviders() } returns flow {
                emit(successResult)
                emit(failureResult)
            }
            coEvery { updateProvider(listOf(testProvider1, testProvider2)) } returns ProviderUpdateResult(
                success = listOf(testProvider1),
                failed = listOf(testProvider2 to error),
            )

            createViewModel()
            advanceUntilIdle()

            viewModel.onUseProfile(testUser1)
            advanceUntilIdle()

            viewModel.uiState.test {
                val finalState = awaitItem()
                expectThat(finalState) {
                    get { isLoggedIn }.isTrue()
                    get { isLoading }.isFalse()
                    get { errors }.hasSize(1)
                    get { errors[testProvider2.id]?.throwable }.isEqualTo(error)
                }
            }
        }
}
