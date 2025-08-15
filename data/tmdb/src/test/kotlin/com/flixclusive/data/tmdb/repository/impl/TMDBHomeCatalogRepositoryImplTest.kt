package com.flixclusive.data.tmdb.repository.impl

import android.content.Context
import android.content.res.AssetManager
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.tmdb.TMDBTestDefaults
import com.flixclusive.data.tmdb.model.TMDBHomeCatalogs
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
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
                    "type": "all",
                    "required": true,
                    "canPaginate": true,
                    "query": "trending/all/day?language=en-US"
                }
            ],
            "movie": [
                {
                    "name": "Top Movies Recently",
                    "type": "movie",
                    "required": true,
                    "canPaginate": true,
                    "query": "discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&primary_release_date.gte=1990-01-01&vote_count.gte=200"
                },
                {
                    "name": "Modern Movie Classics",
                    "type": "movie",
                    "required": false,
                    "canPaginate": true,
                    "query": "discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=vote_average.desc&vote_count.gte=300"
                }
            ],
            "tv": [
                {
                    "name": "Popular TV Shows",
                    "type": "tv",
                    "required": true,
                    "canPaginate": true,
                    "query": "discover/tv?sort_by=popularity.desc"
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
            context,
            tmdbApiService,
            appDispatchers,
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
    fun `getCatalogsForMediaType returns correct catalogs for movie type`() =
        runTest(testDispatcher) {
            setupSuccessfulAssetReading()

            val result = repository.getCatalogsForMediaType("movie")

            expectThat(result).hasSize(2)
            expectThat(result[0]).get { name }.isEqualTo("Top Movies Recently")
            expectThat(result[1]).get { name }.isEqualTo("Modern Movie Classics")
        }

    @Test
    fun `getRequiredCatalogsForMediaType returns only required catalogs`() =
        runTest(testDispatcher) {
            setupSuccessfulAssetReading()

            val result = repository.getRequiredCatalogsForMediaType("movie")

            expectThat(result).hasSize(1)
            expectThat(result[0]).get { name }.isEqualTo("Top Movies Recently")
            expectThat(result[0]).get { required }.isTrue()
        }

    @Test
    fun `getTrending returns success with real API data`() =
        runTest(testDispatcher) {
            val mediaType = "all"
            val timeWindow = "week"
            val page = 1

            val result = repository.getTrending(mediaType, timeWindow, page)

            expectThat(result).isA<Resource.Success<SearchResponseData<FilmSearchItem>>>()
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

            expectThat(result).isA<Resource.Success<SearchResponseData<FilmSearchItem>>>()
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

            expectThat(result).isA<Resource.Success<SearchResponseData<FilmSearchItem>>>()
            val response = (result as Resource.Success).data
            expectThat(response).isNotNull()
            expectThat(response!!) {
                get { results }.isNotEmpty()
            }
        }

    private fun setupSuccessfulAssetReading() {
        val inputStream = ByteArrayInputStream(validHomeCatalogsJson.toByteArray())
        every { assetManager.open("home_catalogs.json") } returns inputStream
    }
}
