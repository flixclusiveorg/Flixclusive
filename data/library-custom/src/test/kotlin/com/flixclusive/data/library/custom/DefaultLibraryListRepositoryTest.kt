package com.flixclusive.data.library.custom

import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.retrofit.TMDB_API_BASE_URL
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.database.LibraryItemId
import com.flixclusive.model.database.LibraryList
import com.flixclusive.model.database.LibraryListItem
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DefaultLibraryListRepositoryTest {
    @get:Rule
    val rule = LogRule()

    private val ownerId = 1
    private lateinit var libraryListRepository: LibraryListRepository
    private lateinit var tmdbRepository: TMDBRepository
    private lateinit var tmdbApiService: TMDBApiService
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        client = OkHttpClient.Builder().build()
        tmdbApiService = client.toTmdbApiService()
        tmdbRepository = FakeTMDBRepository(
            client = client,
            tmdbApiService = tmdbApiService
        )

        libraryListRepository = DefaultLibraryListRepository(
            tmdbRepository = tmdbRepository,
            dataSource = FakeLibraryListDataSource()
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `test write and read`() = runTest {
        val listId = Uuid.random().toString()
        val list = LibraryList(
            id = listId,
            ownerId = ownerId,
            name = "Test catalog"
        )

        libraryListRepository.createList(list)
        val libraryList = libraryListRepository.getList(listId)
        assert(libraryList != null)
        assert(list == libraryList)
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `test write, remove then read`() = runTest {
        val listId = Uuid.random().toString()
        val list = LibraryList(
            id = listId,
            ownerId = ownerId,
            name = "Test catalog"
        )

        libraryListRepository.createList(list)
        libraryListRepository.removeList(list)
        val libraryList = libraryListRepository.getList(listId)
        assert(libraryList == null)
    }

    @Test
    fun `test read lists`() = runTest {
        val list = libraryListRepository.getLists(ownerId)
        assert(list.isNotEmpty())

        val flowList = libraryListRepository.observeLists(ownerId).first()
        assert(flowList.isNotEmpty())
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `test add item to list and read`() = runTest {
        val listId = Uuid.random().toString()
        val entryId = 999L
        val imdbId = "tt20292092"

        val list = LibraryList(
            id = listId,
            ownerId = ownerId,
            name = "Test catalog"
        )

        libraryListRepository.createList(list)
        val listItem = LibraryListItem(
            entryId = entryId,
            listId = listId,
            libraryItemId = LibraryItemId(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                itemId = imdbId
            )
        )

        libraryListRepository.addItemToList(listItem)

        val item = libraryListRepository.getListItem(entryId)
        assert(item != null)
        val flowItem = libraryListRepository.observeListItem(entryId).first()
        assert(flowItem != null)
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `test add, read and read item to list`() = runTest {
        val listId = Uuid.random().toString()
        val entryId = 999L
        val imdbId = "tt20292092"

        val list = LibraryList(
            id = listId,
            ownerId = ownerId,
            name = "Test catalog"
        )

        libraryListRepository.createList(list)
        val listItem = LibraryListItem(
            entryId = entryId,
            listId = listId,
            libraryItemId = LibraryItemId(
                providerId = DEFAULT_FILM_SOURCE_NAME,
                itemId = imdbId
            )
        )

        libraryListRepository.addItemToList(listItem)
        libraryListRepository.removeItemFromList(listItem)

        val item = libraryListRepository.getListItem(entryId)
        assert(item == null)
        val flowItem = libraryListRepository.observeListItem(entryId).first()
        assert(flowItem == null)
    }
}

private fun OkHttpClient.toTmdbApiService(): TMDBApiService {
    return Retrofit.Builder()
        .baseUrl(TMDB_API_BASE_URL)
        .client(this)
        .build()
        .create(TMDBApiService::class.java)
}
