package com.flixclusive.data.tmdb.repository.impl

import android.content.Context
import android.content.res.AssetManager
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.extensions.isSuccess
import com.flixclusive.core.testing.tmdb.TMDBTestDefaults
import com.flixclusive.data.tmdb.model.TMDBHomeCatalogs
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.io.ByteArrayInputStream

class TMDBHomeCatalogRepositoryImplTest {
    private val context: Context = mockk()
    private val assetManager: AssetManager = mockk()
    private lateinit var tmdbApiService: TMDBApiService
    private lateinit var appDispatchers: AppDispatchers

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TMDBHomeCatalogRepositoryImpl

    private val validHomeCatalogsJson = """
        {
            "all": [
                {
                    "name": "Trending Now",
                    "required": true,
                    "url": "https://api.themoviedb.org/3/trending/all/day?language=en-US"
                }
            ],
            "movie": [
                {
                    "name": "Top Movies Recently",
                    "required": true,
                    "url": "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&primary_release_date.gte=1990-01-01&vote_count.gte=200"
                },
                {
                    "name": "Modern Movie Classics",
                    "required": false,
                    "url": "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=vote_average.desc&vote_count.gte=300"
                }
            ],
            "tv": [
                {
                    "name": "Popular TV Shows",
                    "required": true,
                    "url": "https://api.themoviedb.org/3/discover/tv?sort_by=popularity.desc"
                }
            ]
        }
    """.trimIndent()

    @Before
    fun setup() {
        every { context.assets } returns assetManager
        tmdbApiService = TMDBTestDefaults.createTMDBApiService()
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        repository = TMDBHomeCatalogRepositoryImpl(
            context = context,
            tmdbApiService = tmdbApiService,
            appDispatchers = appDispatchers,
        )
    }

    @Test
    fun `read parses home catalogs JSON correctly when valid asset file is provided`() =
        runTest(testDispatcher) {
            val inputStream = ByteArrayInputStream(validHomeCatalogsJson.toByteArray())
            every { assetManager.open("test.json") } returns inputStream

            val result = repository.read("test.json")

            expectThat(result).isA<TMDBHomeCatalogs>()
            expectThat(result.all).hasSize(1)
            expectThat(result.movie).hasSize(2)
            expectThat(result.tv).hasSize(1)
        }

    @Test
    fun `getTrending returns success with real API data`() =
        runTest(testDispatcher) {
            val mediaType = "all"
            val timeWindow = "week"
            val page = 1

            val result = repository.getTrending(mediaType, timeWindow, page)

            expectThat(result).isSuccess()
            val response = (result as Resource.Success).data
            expectThat(response).isNotNull()
            expectThat(response!!) {
                get { results }.isNotEmpty()
                get { this.page }.isEqualTo(page)
                get { totalPages }.isGreaterThan(0)
            }
        }

    @Test
    fun `getTrending returns success for movies with real API data`() =
        runTest(testDispatcher) {
            val mediaType = "movie"
            val timeWindow = "day"
            val page = 1

            val result = repository.getTrending(mediaType, timeWindow, page)

            expectThat(result).isSuccess()
            val response = (result as Resource.Success).data
            expectThat(response).isNotNull()
            expectThat(response!!) {
                get { results }.isNotEmpty()
                get { results.all { it.title.isNotEmpty() } }.isTrue()
            }
        }

    @Test
    fun `getTrending returns success for TV shows with real API data`() =
        runTest(testDispatcher) {
            val mediaType = "tv"
            val timeWindow = "week"
            val page = 1

            val result = repository.getTrending(mediaType, timeWindow, page)

            expectThat(result).isSuccess()
            val response = (result as Resource.Success).data
            expectThat(response).isNotNull()
            expectThat(response!!) {
                get { results }.isNotEmpty()
            }
        }
}
