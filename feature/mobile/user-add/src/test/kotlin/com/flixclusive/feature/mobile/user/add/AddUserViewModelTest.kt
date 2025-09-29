package com.flixclusive.feature.mobile.user.add

import app.cash.turbine.test
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.data.database.repository.UserRepository
import com.flixclusive.data.database.session.UserSessionManager
import com.flixclusive.data.tmdb.repository.TMDBAssetsRepository
import com.flixclusive.data.tmdb.repository.TMDBHomeCatalogRepository
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class AddUserViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appDispatchers: AppDispatchers

    private val userRepository: UserRepository = mockk()
    private val userSessionManager: UserSessionManager = mockk()
    private val tmdbHomeCatalogRepository: TMDBHomeCatalogRepository = mockk()
    private val tmdbAssetsRepository: TMDBAssetsRepository = mockk()

    private lateinit var viewModel: AddUserViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be NotAdded`() =
        runTest(testDispatcher) {
            coEvery { tmdbHomeCatalogRepository.getTrending(any(), any(), any()) } returns Resource.Loading

            viewModel = createViewModel()

            viewModel.state.test {
                expectThat(awaitItem()).isA<AddUserState.NotAdded>()
            }
        }

    @Test
    fun `should initialize with empty user`() =
        runTest(testDispatcher) {
            coEvery { tmdbHomeCatalogRepository.getTrending(any(), any(), any()) } returns Resource.Loading

            viewModel = createViewModel()

            expectThat(viewModel.user.value).isEqualTo(User.EMPTY)
        }

    @Test
    fun `should use default backgrounds when trending fails`() =
        runTest(testDispatcher) {
            coEvery { tmdbHomeCatalogRepository.getTrending(any(), any(), any()) } returns
                Resource.Failure("Network error")

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.images.test {
                val images = awaitItem()
                expectThat(images.size).isEqualTo(6)
            }
        }

    @Test
    fun `should use backdrop image when poster fails`() =
        runTest(testDispatcher) {
            val mockFilm = FilmTestDefaults.getFilmSearchItem(
                tmdbId = 123,
                backdropImage = "/test-backdrop.jpg",
            )
            val mockSearchResponseData = mockk<SearchResponseData<FilmSearchItem>> {
                every { results } returns List(10) {
                    mockFilm.copy(
                        tmdbId = 123 + it,
                        backdropImage = "/test-backdrop-$it.jpg",
                    )
                }
            }

            coEvery { tmdbHomeCatalogRepository.getTrending(any(), any(), any()) } returns Resource.Success(
                mockSearchResponseData,
            )
            coEvery { tmdbAssetsRepository.getPosterWithoutLogo(any(), any()) } returns
                Resource.Failure("Poster not found")

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.images.test {
                val images = awaitItem()
                expectThat(images).hasSize(3)
            }
        }

    @Test
    fun `should use default backgrounds when no results from trending`() =
        runTest(testDispatcher) {
            val mockSearchResponseData = mockk<SearchResponseData<FilmSearchItem>> {
                every { results } returns emptyList()
            }

            coEvery { tmdbHomeCatalogRepository.getTrending(any(), any(), any()) } returns Resource.Success(
                mockSearchResponseData,
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.images.test {
                val images = awaitItem()
                expectThat(images.size).isEqualTo(6)
            }
        }

    @Test
    fun `addUser should add user and update state to Added`() =
        runTest(testDispatcher) {
            val testUser = User(0, "Test User", 1, null, null)
            val expectedUserId = 123L

            coEvery { tmdbHomeCatalogRepository.getTrending(any(), any(), any()) } returns Resource.Loading
            coEvery { userRepository.addUser(any()) } returns expectedUserId

            viewModel = createViewModel()

            viewModel.addUser(testUser, isSigningIn = false)
            advanceUntilIdle()

            viewModel.state.test {
                expectThat(awaitItem()).isA<AddUserState.Added>()
            }

            coVerify { userRepository.addUser(testUser) }
        }

    @Test
    fun `addUser with sign in should add user and sign in`() =
        runTest(testDispatcher) {
            val testUser = User(0, "Test User", 1, null, null)
            val expectedUserId = 123L
            val expectedSignedInUser = testUser.copy(id = expectedUserId.toInt())

            coEvery { tmdbHomeCatalogRepository.getTrending(any(), any(), any()) } returns Resource.Loading
            coEvery { userRepository.addUser(any()) } returns expectedUserId
            coEvery { userSessionManager.signIn(any()) } returns Unit

            viewModel = createViewModel()

            viewModel.addUser(testUser, isSigningIn = true)
            advanceUntilIdle()

            viewModel.state.test {
                expectThat(awaitItem()).isA<AddUserState.Added>()
            }

            coVerify { userRepository.addUser(testUser) }
            coVerify { userSessionManager.signIn(expectedSignedInUser) }
        }

    @Test
    fun `addUser should not execute if already active`() =
        runTest(testDispatcher) {
            val testUser = User(0, "Test User", 1, null, null)

            coEvery { tmdbHomeCatalogRepository.getTrending(any(), any(), any()) } returns Resource.Loading
            coEvery { userRepository.addUser(any()) } returns 123L

            viewModel = createViewModel()

            // Start first job
            viewModel.addUser(testUser, isSigningIn = false)

            // Try to start second job before first completes
            viewModel.addUser(testUser, isSigningIn = false)

            advanceUntilIdle()

            // Verify addUser was only called once
            coVerify(exactly = 1) { userRepository.addUser(any()) }
        }

    @Test
    fun `should handle null tmdbId gracefully`() =
        runTest(testDispatcher) {
            val mockFilm = FilmTestDefaults.getFilmSearchItem(tmdbId = null)
            val mockSearchResponseData = mockk<SearchResponseData<FilmSearchItem>> {
                every { results } returns listOf(mockFilm)
            }

            coEvery { tmdbHomeCatalogRepository.getTrending(any(), any(), any()) } returns Resource.Success(
                mockSearchResponseData,
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.images.test {
                val images = awaitItem()
                expectThat(images.size).isEqualTo(6) // Should fall back to default backgrounds
            }
        }

    @Test
    fun `should handle empty trending results gracefully`() =
        runTest(testDispatcher) {
            val mockSearchResponseData = mockk<SearchResponseData<FilmSearchItem>> {
                every { results } returns emptyList()
            }

            coEvery { tmdbHomeCatalogRepository.getTrending(any(), any(), any()) } returns Resource.Success(
                mockSearchResponseData,
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.images.test {
                val images = awaitItem()
                expectThat(images.size).isEqualTo(6) // Should fall back to default backgrounds
            }
        }

    private fun createViewModel(): AddUserViewModel {
        return AddUserViewModel(
            userRepository = userRepository,
            userSessionManager = userSessionManager,
            tmdbHomeCatalogRepository = tmdbHomeCatalogRepository,
            tmdbAssetsRepository = tmdbAssetsRepository,
            appDispatchers = appDispatchers,
        )
    }
}
