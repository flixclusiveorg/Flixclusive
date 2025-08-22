package com.flixclusive.data.tmdb.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.extensions.isFailure
import com.flixclusive.core.testing.extensions.isSuccess
import com.flixclusive.core.testing.tmdb.TMDBTestDefaults
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull

class TMDBFilmSearchItemsRepositoryImplTest {
    private lateinit var tmdbApiService: TMDBApiService
    private lateinit var appDispatchers: AppDispatchers
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TMDBFilmSearchItemsRepositoryImpl

    @Before
    fun setup() {
        tmdbApiService = TMDBTestDefaults.createTMDBApiService()
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        repository = TMDBFilmSearchItemsRepositoryImpl(
            tmdbApiService,
            appDispatchers,
        )
    }

    @Test
    fun `search returns success with real API data for valid query`() =
        runTest(testDispatcher) {
            val query = "Inception"
            val page = 1
            val filter = 0

            val result = repository.search(query, page, filter)

            expectThat(result).isSuccess()
            val response = (result as Resource.Success).data
            expectThat(response).isNotNull()
            expectThat(response!!.results).isNotEmpty()
            expectThat(response.results.size).isGreaterThan(0)
        }

    @Test
    fun `search returns success with real API data for TV show query`() =
        runTest(testDispatcher) {
            val query = "Breaking Bad"
            val page = 1
            val filter = 2 // TV filter

            val result = repository.search(query, page, filter)

            expectThat(result).isSuccess()
            val response = (result as Resource.Success).data
            expectThat(response).isNotNull()
            expectThat(response!!.results).isNotEmpty()
        }

    @Test
    fun `search returns failure when query is empty`() =
        runTest(testDispatcher) {
            val query = ""
            val page = 1
            val filter = 0

            val result = repository.search(query, page, filter)

            expectThat(result).isFailure()
            expectThat((result as Resource.Failure).error).isA<UiText.StringValue>()
        }

    @Test
    fun `get returns success with real API data for valid URL`() =
        runTest(testDispatcher) {
            val url =
                "discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&primary_release_date.gte=1990-01-01&vote_count.gte=200&with_genres=28"
            val page = 1

            val result = repository.get(url, page)

            expectThat(result).isSuccess()
            val response = (result as Resource.Success).data
            expectThat(response).isNotNull()
            expectThat(response!!.results).isNotEmpty()
        }

    @Test
    fun `get returns failure for invalid URL`() =
        runTest(testDispatcher) {
            val url = "invalid/url/format"
            val page = 1

            val result = repository.get(url, page)

            expectThat(result).isFailure()
            expectThat((result as Resource.Failure).error).isA<UiText.StringValue>()
        }
}
