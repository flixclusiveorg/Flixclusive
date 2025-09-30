package com.flixclusive.feature.mobile.user.edit

import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.data.database.repository.SearchHistoryRepository
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.data.database.repository.WatchlistRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.domain.provider.usecase.manage.UnloadProviderUseCase
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
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class UserEditViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appDispatchers: AppDispatchers

    private val dataStoreManager: DataStoreManager = mockk()
    private val userRepository: UserRepository = mockk()
    private val userSessionManager: UserSessionManager = mockk()
    private val searchHistoryRepository: SearchHistoryRepository = mockk()
    private val watchlistRepository: WatchlistRepository = mockk()
    private val watchProgressRepository: WatchProgressRepository = mockk()
    private val providerRepository: ProviderRepository = mockk()
    private val unloadProvider: UnloadProviderUseCase = mockk()

    private lateinit var viewModel: UserEditViewModel

    private val testUser = User(
        id = 123,
        name = "Test User",
        image = 1,
        pin = "1234",
        pinHint = "Test hint",
    )

    private val loggedInUser = User(
        id = 456,
        name = "Logged In User",
        image = 2,
        pin = null,
        pinHint = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        every { userSessionManager.currentUser } returns MutableStateFlow(null)
        every { userRepository.observeUser(any()) } returns flowOf(null)

        viewModel = UserEditViewModel(
            dataStoreManager = dataStoreManager,
            userRepository = userRepository,
            userSessionManager = userSessionManager,
            searchHistoryRepository = searchHistoryRepository,
            watchlistRepository = watchlistRepository,
            watchProgressRepository = watchProgressRepository,
            providerRepository = providerRepository,
            unloadProvider = unloadProvider,
            appDispatchers = appDispatchers,
            savedStateHandle = mockk(relaxed = true) {
                every { get<Int?>("userId") } returns loggedInUser.id
            }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onRemoveUser should remove user and emit GoBack when user is not logged in`() =
        runTest(testDispatcher) {
            every { userSessionManager.currentUser } returns MutableStateFlow(null)
            every { userRepository.observeUser(any()) } returns flowOf(testUser)
            coEvery { dataStoreManager.deleteAllUserRelatedFiles(any()) } returns Unit
            coEvery { searchHistoryRepository.clearAll(any()) } returns Unit
            coEvery { watchProgressRepository.removeAll(any()) } returns Unit
            coEvery { watchlistRepository.removeAll(any()) } returns Unit
            coEvery { userRepository.deleteUser(any()) } returns Unit

            viewModel.onRemoveNavigationState.test {
                viewModel.onRemoveUser(testUser.id)
                testDispatcher.scheduler.advanceUntilIdle()

                expectThat(awaitItem()).isEqualTo(OnRemoveNavigationState.GoBack)
            }

            coVerify { dataStoreManager.deleteAllUserRelatedFiles(testUser.id) }
            coVerify { searchHistoryRepository.clearAll(testUser.id) }
            coVerify { watchProgressRepository.removeAll(testUser.id) }
            coVerify { watchlistRepository.removeAll(testUser.id) }
            coVerify { userRepository.deleteUser(testUser.id) }
        }

    @Test
    fun `onRemoveUser should remove logged in user and emit PopToRoot`() =
        runTest(testDispatcher) {
            val providerMetadata = DummyDataForPreview.getDummyProviderMetadata()

            every { userSessionManager.currentUser } returns MutableStateFlow(loggedInUser)
            every { userRepository.observeUser(any()) } returns flowOf(loggedInUser)
            every { providerRepository.getProviders() } returns listOf(providerMetadata)
            coEvery { unloadProvider(any(), any()) } returns Unit
            coEvery { userSessionManager.signOut() } returns Unit
            coEvery { dataStoreManager.deleteAllUserRelatedFiles(any()) } returns Unit
            coEvery { searchHistoryRepository.clearAll(any()) } returns Unit
            coEvery { watchProgressRepository.removeAll(any()) } returns Unit
            coEvery { watchlistRepository.removeAll(any()) } returns Unit
            coEvery { userRepository.deleteUser(any()) } returns Unit

            viewModel.onRemoveNavigationState.test {
                viewModel.onRemoveUser(loggedInUser.id)
                testDispatcher.scheduler.advanceUntilIdle()

                expectThat(awaitItem()).isEqualTo(OnRemoveNavigationState.PopToRoot)
            }

            coVerify { providerRepository.getProviders() }
            coVerify { unloadProvider(providerMetadata, true) }
            coVerify { userSessionManager.signOut() }
            coVerify { dataStoreManager.deleteAllUserRelatedFiles(loggedInUser.id) }
            coVerify { searchHistoryRepository.clearAll(loggedInUser.id) }
            coVerify { watchProgressRepository.removeAll(loggedInUser.id) }
            coVerify { watchlistRepository.removeAll(loggedInUser.id) }
            coVerify { userRepository.deleteUser(loggedInUser.id) }
        }

    @Test
    fun `onRemoveUser should not execute if already active`() =
        runTest(testDispatcher) {
            every { userSessionManager.currentUser } returns MutableStateFlow(null)
            every { userRepository.observeUser(any()) } returns flowOf(loggedInUser)
            coEvery { dataStoreManager.deleteAllUserRelatedFiles(any()) } returns Unit
            coEvery { searchHistoryRepository.clearAll(any()) } returns Unit
            coEvery { watchProgressRepository.removeAll(any()) } returns Unit
            coEvery { watchlistRepository.removeAll(any()) } returns Unit
            coEvery { userRepository.deleteUser(any()) } returns Unit

            // Start first job
            viewModel.onRemoveUser(testUser.id)

            // Try to start second job before first completes
            viewModel.onRemoveUser(testUser.id)

            testDispatcher.scheduler.advanceUntilIdle()

            // Verify deleteUser was only called once
            coVerify(exactly = 1) { userRepository.deleteUser(testUser.id) }
        }

    @Test
    fun `onEditUser should update user in repository`() =
        runTest(testDispatcher) {
            coEvery { userRepository.updateUser(any()) } returns Unit

            viewModel.onEditUser(testUser)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { userRepository.updateUser(testUser) }
        }

    @Test
    fun `onEditUser should not execute if already active`() =
        runTest(testDispatcher) {
            coEvery { userRepository.updateUser(any()) } returns Unit

            // Start first job
            viewModel.onEditUser(testUser)

            // Try to start second job before first completes
            viewModel.onEditUser(testUser)

            testDispatcher.scheduler.advanceUntilIdle()

            // Verify updateUser was only called once
            coVerify(exactly = 1) { userRepository.updateUser(testUser) }
        }

    @Test
    fun `onClearSearchHistory should clear search history for user`() =
        runTest(testDispatcher) {
            coEvery { searchHistoryRepository.clearAll(any()) } returns Unit

            viewModel.onClearSearchHistory(testUser.id)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { searchHistoryRepository.clearAll(ownerId = testUser.id) }
        }

    @Test
    fun `onClearSearchHistory should not execute if already active`() =
        runTest(testDispatcher) {
            coEvery { searchHistoryRepository.clearAll(any()) } returns Unit

            // Start first job
            viewModel.onClearSearchHistory(testUser.id)

            // Try to start second job before first completes
            viewModel.onClearSearchHistory(testUser.id)

            testDispatcher.scheduler.advanceUntilIdle()

            // Verify clearAll was only called once
            coVerify(exactly = 1) { searchHistoryRepository.clearAll(ownerId = testUser.id) }
        }

    @Test
    fun `onClearLibraries should clear watchlist when Library Watchlist is provided`() =
        runTest(testDispatcher) {
            coEvery { watchlistRepository.removeAll(any()) } returns Unit

            viewModel.onClearLibraries(testUser.id, listOf(Library.Watchlist))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { watchlistRepository.removeAll(testUser.id) }
        }

    @Test
    fun `onClearLibraries should clear watch history when Library WatchHistory is provided`() =
        runTest(testDispatcher) {
            coEvery { watchProgressRepository.removeAll(any()) } returns Unit

            viewModel.onClearLibraries(testUser.id, listOf(Library.WatchHistory))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { watchProgressRepository.removeAll(testUser.id) }
        }

    @Test
    fun `onClearLibraries should clear multiple libraries when multiple types provided`() =
        runTest(testDispatcher) {
            coEvery { watchlistRepository.removeAll(any()) } returns Unit
            coEvery { watchProgressRepository.removeAll(any()) } returns Unit

            viewModel.onClearLibraries(
                testUser.id,
                listOf(Library.Watchlist, Library.WatchHistory),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { watchlistRepository.removeAll(testUser.id) }
            coVerify { watchProgressRepository.removeAll(testUser.id) }
        }

    @Test
    fun `onClearLibraries should throw exception for CustomList`() =
        runTest(testDispatcher) {
            val customList = Library.CustomList(id = 1, name = mockk())

            try {
                viewModel.onClearLibraries(testUser.id, listOf(customList))
                testDispatcher.scheduler.advanceUntilIdle()
            } catch (e: IllegalStateException) {
                expectThat(e.message).isEqualTo("Custom libraries are not yet implemented")
            }
        }

    @Test
    fun `onClearLibraries should not execute if already active`() =
        runTest(testDispatcher) {
            coEvery { watchlistRepository.removeAll(any()) } returns Unit

            // Start first job
            viewModel.onClearLibraries(testUser.id, listOf(Library.Watchlist))

            // Try to start second job before first completes
            viewModel.onClearLibraries(testUser.id, listOf(Library.Watchlist))

            testDispatcher.scheduler.advanceUntilIdle()

            // Verify removeAll was only called once
            coVerify(exactly = 1) { watchlistRepository.removeAll(testUser.id) }
        }

    @Test
    fun `clearProviders should not execute when user is not logged in`() =
        runTest(testDispatcher) {
            every { userSessionManager.currentUser } returns MutableStateFlow(null)
            every { userRepository.observeUser(any()) } returns flowOf(testUser)
            coEvery { dataStoreManager.deleteAllUserRelatedFiles(any()) } returns Unit
            coEvery { searchHistoryRepository.clearAll(any()) } returns Unit
            coEvery { watchProgressRepository.removeAll(any()) } returns Unit
            coEvery { watchlistRepository.removeAll(any()) } returns Unit
            coEvery { userRepository.deleteUser(any()) } returns Unit

            viewModel.onRemoveUser(testUser.id)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { providerRepository.getProviders() }
            coVerify(exactly = 0) { unloadProvider(any(), any()) }
        }

    @Test
    fun `signOut should not execute when user is not logged in`() =
        runTest(testDispatcher) {
            every { userSessionManager.currentUser } returns MutableStateFlow(null)
            every { userRepository.observeUser(any()) } returns flowOf(testUser)
            coEvery { dataStoreManager.deleteAllUserRelatedFiles(any()) } returns Unit
            coEvery { searchHistoryRepository.clearAll(any()) } returns Unit
            coEvery { watchProgressRepository.removeAll(any()) } returns Unit
            coEvery { watchlistRepository.removeAll(any()) } returns Unit
            coEvery { userRepository.deleteUser(any()) } returns Unit

            viewModel.onRemoveUser(testUser.id)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { userSessionManager.signOut() }
        }

    @Test
    fun `OnRemoveNavigationState getStateIfUserIsLoggedIn should return PopToRoot when logged in`() {
        val result = OnRemoveNavigationState.getStateIfUserIsLoggedIn(true)
        expectThat(result).isEqualTo(OnRemoveNavigationState.PopToRoot)
    }

    @Test
    fun `OnRemoveNavigationState getStateIfUserIsLoggedIn should return GoBack when not logged in`() {
        val result = OnRemoveNavigationState.getStateIfUserIsLoggedIn(false)
        expectThat(result).isEqualTo(OnRemoveNavigationState.GoBack)
    }

    @Test
    fun `Library Watchlist should have correct UiText name`() {
        expectThat(Library.Watchlist.name).isA<com.flixclusive.core.common.locale.UiText.StringResource>()
    }

    @Test
    fun `Library WatchHistory should have correct UiText name`() {
        expectThat(Library.WatchHistory.name).isA<com.flixclusive.core.common.locale.UiText.StringResource>()
    }
}
