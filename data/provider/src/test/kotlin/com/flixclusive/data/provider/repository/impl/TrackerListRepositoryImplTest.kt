package com.flixclusive.data.provider.repository.impl

import android.content.Context
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.data.provider.repository.ProviderRepository
import com.flixclusive.data.provider.repository.ProviderResponseWrapper
import com.flixclusive.data.provider.repository.TrackerListKey
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.provider.ProviderPlugin
import com.flixclusive.provider.capability.TrackerFeature
import com.flixclusive.provider.capability.TrackerProviderApi
import com.flixclusive.provider.tracker.ScrobbleAction
import com.flixclusive.provider.tracker.TrackerList
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class TrackerListRepositoryImplTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var providerRepository: ProviderRepository
    private lateinit var userSessionDataStore: UserSessionDataStore
    private lateinit var repository: TrackerListRepositoryImpl

    private val providerId = "tracker-1"
    private val ownerId = "owner-1"

    private val listA = TrackerList(id = "list-a", providerId = providerId, name = "Watching")
    private val listB = TrackerList(id = "list-b", providerId = providerId, name = "Planned")

    private val media = Movie(
        id = "media-1",
        title = "A Movie",
        providerId = providerId,
        posterImage = null,
    )

    private class FakeTrackerApi(
        var lists: List<TrackerList>,
        var pages: List<PaginatedMedia<MediaMetadata>> = emptyList(),
        var inList: Boolean = false,
    ) : TrackerProviderApi {
        var getListsCalls = 0
        var addCalls = 0
        var removeCalls = 0

        override suspend fun getFeatures() = setOf(TrackerFeature.LIST_MANAGEMENT)

        override suspend fun isAuthenticated() = true

        override suspend fun isInList(list: TrackerList, item: MediaMetadata) = inList

        override suspend fun getLists(): List<TrackerList> {
            getListsCalls++
            return lists
        }

        override suspend fun getList(id: String) = lists.first { it.id == id }

        override suspend fun createList(name: String, description: String?): TrackerList {
            val created = TrackerList(id = "created", providerId = "tracker-1", name = name)
            lists = lists + created
            return created
        }

        override suspend fun updateList(list: TrackerList) = list

        override suspend fun deleteList(list: TrackerList) {
            lists = lists.filterNot { it.id == list.id }
        }

        override suspend fun getListItems(
            list: TrackerList,
            page: Int,
            pageSize: Int,
        ): PaginatedMedia<MediaMetadata> = pages[page - 1]

        override suspend fun addListItem(list: TrackerList, item: MediaMetadata) {
            addCalls++
            inList = true
        }

        override suspend fun removeListItem(list: TrackerList, item: MediaMetadata) {
            removeCalls++
            inList = false
        }

        override suspend fun scrobble(
            action: ScrobbleAction,
            media: MediaMetadata,
            progressPercent: Float,
            atMs: Long?,
            episode: Episode?,
        ) = Unit

        override suspend fun getScrobbledProgress(item: MediaMetadata, episode: Episode?) = 0f
    }

    private fun givenApi(api: TrackerProviderApi) {
        val plugin = mockk<ProviderPlugin>()
        coEvery { plugin.getTrackerApi(context) } returns api

        val installed = InstalledProvider(
            id = providerId,
            ownerId = ownerId,
            repositoryUrl = "https://example.com",
            filePath = "/tmp/$providerId",
        )

        coEvery { providerRepository.getProvider(providerId, ownerId) } returns
            ProviderResponseWrapper(provider = installed, plugin = plugin, metadata = null)
    }

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.i(any(), any()) } returns 0

        context = mockk(relaxed = true)
        providerRepository = mockk()
        userSessionDataStore = mockk()

        every { userSessionDataStore.currentUserId } returns flowOf(ownerId)
        every { context.getString(any()) } returns "error"
        every { context.getString(any(), any()) } returns "error"

        repository = TrackerListRepositoryImpl(
            context = context,
            providerRepository = providerRepository,
            userSessionDataStore = userSessionDataStore,
            appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
        )
    }

    @Test
    fun `loadLists caches and skips a second load`() = runTest(testDispatcher) {
        val api = FakeTrackerApi(lists = listOf(listA, listB))
        givenApi(api)

        repository.loadLists(providerId)
        repository.loadLists(providerId)

        expectThat(api.getListsCalls).isEqualTo(1)
        expectThat(repository.getLists(providerId).first()).isA<Async.Success<List<TrackerList>>>()
    }

    @Test
    fun `loadLists refetches when refresh is requested`() = runTest(testDispatcher) {
        val api = FakeTrackerApi(lists = listOf(listA))
        givenApi(api)

        repository.loadLists(providerId)
        repository.loadLists(providerId, refresh = true)

        expectThat(api.getListsCalls).isEqualTo(2)
    }

    @Test
    fun `loadLists surfaces a failure without throwing`() = runTest(testDispatcher) {
        val api = mockk<TrackerProviderApi>()
        coEvery { api.getFeatures() } returns setOf(TrackerFeature.LIST_MANAGEMENT)
        coEvery { api.isAuthenticated() } returns true
        coEvery { api.getLists() } throws IllegalStateException("boom")
        givenApi(api)

        repository.loadLists(providerId)

        expectThat(repository.getLists(providerId).first()).isA<Async.Failure>()
    }

    @Test
    fun `loadNextItems accumulates pages`() = runTest(testDispatcher) {
        val api = FakeTrackerApi(
            lists = listOf(listA),
            pages = listOf(
                PaginatedMedia(page = 1, results = listOf(media), hasNextPage = true),
                PaginatedMedia(page = 2, results = listOf(media.copy(id = "media-2")), hasNextPage = false),
            ),
        )
        givenApi(api)

        repository.loadNextItems(listA)
        repository.loadNextItems(listA)

        val key = TrackerListKey(providerId = providerId, listId = listA.id)
        val state = repository.getItems(key).first()

        expectThat(state).isA<Async.Success<*>>()
        val data = (state as Async.Success).data
        expectThat(data.items).hasSize(2)
        expectThat(data.page).isEqualTo(2)
        expectThat(data.hasNextPage).isFalse()
    }

    @Test
    fun `loadNextItems stops once exhausted`() = runTest(testDispatcher) {
        val api = FakeTrackerApi(
            lists = listOf(listA),
            pages = listOf(PaginatedMedia(page = 1, results = listOf(media), hasNextPage = false)),
        )
        givenApi(api)

        repository.loadNextItems(listA)
        repository.loadNextItems(listA)

        val key = TrackerListKey(providerId = providerId, listId = listA.id)
        expectThat(((repository.getItems(key).first()) as Async.Success).data.items).hasSize(1)
    }

    @Test
    fun `addItem then removeItem flips membership`() = runTest(testDispatcher) {
        val api = FakeTrackerApi(lists = listOf(listA))
        givenApi(api)

        val key = TrackerListKey(providerId = providerId, listId = listA.id)

        repository.addItem(mediaId = media.id, list = listA, media = media)
        expectThat(repository.getMembership(media.id).first()[key]).isTrue()

        repository.removeItem(mediaId = media.id, list = listA, media = media)
        expectThat(repository.getMembership(media.id).first()[key]).isFalse()

        expectThat(api.addCalls).isEqualTo(1)
        expectThat(api.removeCalls).isEqualTo(1)
    }

    @Test
    fun `a membership write leaves the lists cache untouched`() = runTest(testDispatcher) {
        val api = FakeTrackerApi(lists = listOf(listA))
        givenApi(api)

        repository.loadLists(providerId)
        val before = repository.getLists(providerId).first()

        repository.loadMembership(mediaId = media.id, list = listA, media = media)

        expectThat(repository.getLists(providerId).first()).isEqualTo(before)
        expectThat(api.getListsCalls).isEqualTo(1)
    }

    @Test
    fun `addItem rejects a media from another provider`() = runTest(testDispatcher) {
        val api = FakeTrackerApi(lists = listOf(listA))
        givenApi(api)

        val foreign = media.copy(providerId = "someone-else")
        val result = repository.addItem(mediaId = foreign.id, list = listA, media = foreign)

        expectThat(result.isFailure).isTrue()
        expectThat(api.addCalls).isEqualTo(0)
    }

    @Test
    fun `invalidate clears the cache for one provider`() = runTest(testDispatcher) {
        val api = FakeTrackerApi(lists = listOf(listA))
        givenApi(api)

        repository.loadLists(providerId)
        repository.invalidate(providerId)

        expectThat(repository.getLists(providerId).first()).isA<Async.Loading>()
    }
}
