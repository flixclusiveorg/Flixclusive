package com.flixclusive.domain.tmdb.usecase

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.retrofit.TMDB_API_KEY
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.network.util.getSearchItemGson
import com.flixclusive.domain.tmdb.usecase.impl.PaginateTMDBCatalogUseCaseImpl
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class PaginateTMDBCatalogUseCaseImplTest {
    private val okHttpClient: OkHttpClient = mockk()
    private val appDispatchers: AppDispatchers = mockk()
    private val gson: Gson = getSearchItemGson()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var useCase: PaginateTMDBCatalogUseCaseImpl

    private val mockCall: Call = mockk()
    private val mockSearchResponse = SearchResponseData<FilmSearchItem>(
        page = 1,
        results = listOf(mockk(relaxed = true)),
        totalPages = 10,
    )

    private val validJsonResponse = """
        {
            "page": 1,
            "results": [
                {
                    "id": 123,
                    "title": "Test Movie",
                    "overview": "Test overview"
                }
            ],
            "total_pages": 10,
            "total_results": 200
        }
    """.trimIndent()

    @Before
    fun setup() {
        every { appDispatchers.io } returns testDispatcher

        useCase = PaginateTMDBCatalogUseCaseImpl(
            okHttpClient,
            appDispatchers
        )
    }

    @Test
    fun `invoke returns success when HTTP request succeeds`() =
        runTest(testDispatcher) {
            val url = "/discover/movie"
            val page = 1
            val expectedUrl = "https://api.themoviedb.org/3$url&page=$page&api_key=${TMDB_API_KEY}"

            val mockResponse = Response
                .Builder()
                .request(Request.Builder().url(expectedUrl).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(validJsonResponse.toResponseBody())
                .build()

            every { okHttpClient.newCall(any()) } returns mockCall
            coEvery { mockCall.execute() } returns mockResponse
            every { gson.fromJson(validJsonResponse, SearchResponseData::class.java) } returns mockSearchResponse

            val result = useCase.invoke(url, page)

            expectThat(result).isA<Resource.Success<SearchResponseData<FilmSearchItem>>>()
            expectThat((result as Resource.Success).data).isEqualTo(mockSearchResponse)
        }

    @Test
    fun `invoke returns failure when HTTP request fails with error code`() =
        runTest(testDispatcher) {
            val url = "/discover/movie"
            val page = 1
            val expectedUrl = "https://api.themoviedb.org/3$url&page=$page&api_key=8d6d91941230817f7807d643736e8a49"

            val mockResponse = Response
                .Builder()
                .request(Request.Builder().url(expectedUrl).build())
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .build()

            every { okHttpClient.newCall(any()) } returns mockCall
            coEvery { mockCall.execute() } returns mockResponse

            val result = useCase.invoke(url, page)

            expectThat(result).isA<Resource.Failure>()
            expectThat((result as Resource.Failure).error.toString()).isEqualTo("HTTP 404: Not Found")
        }

    @Test
    fun `invoke returns failure when OkHttp throws exception`() =
        runTest(testDispatcher) {
            val url = "/discover/movie"
            val page = 1
            val exception = RuntimeException("Network error")

            every { okHttpClient.newCall(any()) } returns mockCall
            coEvery { mockCall.execute() } throws exception

            val result = useCase.invoke(url, page)

            expectThat(result).isA<Resource.Failure>()
        }

    @Test
    fun `invoke returns failure when response body is null`() =
        runTest(testDispatcher) {
            val url = "/discover/movie"
            val page = 1
            val expectedUrl = "https://api.themoviedb.org/3$url&page=$page&api_key=8d6d91941230817f7807d643736e8a49"

            val mockResponse = Response
                .Builder()
                .request(Request.Builder().url(expectedUrl).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()

            every { okHttpClient.newCall(any()) } returns mockCall
            coEvery { mockCall.execute() } returns mockResponse

            val result = useCase.invoke(url, page)

            expectThat(result).isA<Resource.Failure>()
            expectThat((result as Resource.Failure).error.toString()).isEqualTo("Empty response body")
        }

    @Test
    fun `invoke constructs correct URL with parameters`() =
        runTest(testDispatcher) {
            val url = "/trending/movie/week"
            val page = 5
            val expectedUrl = "https://api.themoviedb.org/3$url&page=$page&api_key=8d6d91941230817f7807d643736e8a49"

            val mockResponse = Response
                .Builder()
                .request(Request.Builder().url(expectedUrl).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(validJsonResponse.toResponseBody())
                .build()

            every { okHttpClient.newCall(any()) } returns mockCall
            coEvery { mockCall.execute() } returns mockResponse
            every { gson.fromJson(validJsonResponse, SearchResponseData::class.java) } returns mockSearchResponse

            useCase.invoke(url, page)

            // Verify the request was made with correct URL
            io.mockk.verify {
                okHttpClient.newCall(
                    match { request ->
                        request.url.toString() == expectedUrl
                    },
                )
            }
        }
}
