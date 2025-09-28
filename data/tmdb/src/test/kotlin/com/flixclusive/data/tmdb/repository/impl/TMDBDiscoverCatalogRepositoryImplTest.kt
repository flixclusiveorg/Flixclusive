package com.flixclusive.data.tmdb.repository.impl

import android.content.Context
import android.content.res.AssetManager
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.data.tmdb.model.TMDBDiscoverCatalog
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import java.io.ByteArrayInputStream
import java.io.IOException

class TMDBDiscoverCatalogRepositoryImplTest {
    private val context: Context = mockk()
    private val assetManager: AssetManager = mockk()
    private val appDispatcher: AppDispatchers = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TMDBDiscoverCatalogRepositoryImpl

    private val mockNetworks = listOf(
        TMDBDiscoverCatalog(
            name = "Cartoon Network",
            image = "/c5OC6oVCg6QP4eqzW6XIq17CQjI.png",
            url =
                "https://api.themoviedb.org/3/discover/tv?include_adult=false&include_video=false" +
                    "&language=en-US&sort_by=popularity.desc" +
                    "&first_air_date.gte=1990-01-01&with_networks=56",
        ),
        TMDBDiscoverCatalog(
            name = "Netflix",
            image = "/wwemzKWzjKYJFfCeiB57q3r4Bcm.png",
            url =
                "https://api.themoviedb.org/3/discover/tv?include_adult=false&include_video=false" +
                    "&language=en-US&sort_by=popularity.desc" +
                    "&first_air_date.gte=1990-01-01&with_networks=213",
        ),
    )

    private val mockCompanies = listOf(
        TMDBDiscoverCatalog(
            name = "Lionsgate",
            image = "/cisLn1YAUuptXVBa0xjq7ST9cH0.png",
            url =
                "https://api.themoviedb.org/3/discover/movie?include_adult=false" +
                    "&include_video=false&language=en-US&sort_by=popularity.desc" +
                    "&primary_release_date.gte=1990-01-01&with_companies=35|1632",
        ),
    )

    private val mockGenres = listOf(
        TMDBDiscoverCatalog(
            name = "Action",
            url = "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false" +
                "&language=en-US&sort_by=popularity.desc" +
                "&primary_release_date.gte=1990-01-01&vote_count.gte=200&with_genres=28",
        ),
        TMDBDiscoverCatalog(
            name = "Action & Adventure",
            url = "https://api.themoviedb.org/3/discover/tv?include_adult=false&include_video=false" +
                "&language=en-US&sort_by=popularity.desc" +
                "&first_air_date.gte=1990-01-01&vote_count.gte=200&with_genres=10759",
        ),
        TMDBDiscoverCatalog(
            name = "Comedy",
            url = "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US" +
                "&sort_by=popularity.desc&primary_release_date.gte=1990-01-01" +
                "&vote_count.gte=200&with_genres=35&without_genres=10763,10767",
        ),
    )

    private val mockTypes = listOf(
        TMDBDiscoverCatalog(
            name = "TV Shows",
            url = "https://api.themoviedb.org/3/discover/tv?include_adult=false&include_video=false" +
                "&language=en-US&sort_by=popularity.desc" +
                "&first_air_date.gte=1990-01-01&vote_count.gte=200&without_genres=10763,10767",
        ),
        TMDBDiscoverCatalog(
            name = "Movies",
            url = "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false" +
                "&language=en-US&sort_by=popularity.desc" +
                "&primary_release_date.gte=1990-01-01&vote_count.gte=200",
        ),
    )

    private val validJsonResponse = """
        {
            "networks": [
                {
                    "name": "Cartoon Network",
                    "image": "/c5OC6oVCg6QP4eqzW6XIq17CQjI.png",
                    "url": "https://api.themoviedb.org/3/discover/tv?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&first_air_date.gte=1990-01-01&with_networks=56"
                },
                {
                    "name": "Netflix",
                    "image": "/wwemzKWzjKYJFfCeiB57q3r4Bcm.png",
                    "url": "https://api.themoviedb.org/3/discover/tv?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&first_air_date.gte=1990-01-01&with_networks=213"
                }
            ],
            "companies": [
                {
                    "name": "Lionsgate",
                    "image": "/cisLn1YAUuptXVBa0xjq7ST9cH0.png",
                    "url": "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&primary_release_date.gte=1990-01-01&with_companies=35|1632"
                }
            ],
            "genres": [
                {
                    "name": "Action",
                    "url": "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&primary_release_date.gte=1990-01-01&vote_count.gte=200&with_genres=28"
                },
                {
                    "name": "Action & Adventure",
                    "url": "https://api.themoviedb.org/3/discover/tv?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&first_air_date.gte=1990-01-01&vote_count.gte=200&with_genres=10759"
                },
                {
                    "name": "Comedy",
                    "url": "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&primary_release_date.gte=1990-01-01&vote_count.gte=200&with_genres=35&without_genres=10763,10767"
                }
            ],
            "type": [
                {
                    "name": "TV Shows",
                    "url": "https://api.themoviedb.org/3/discover/tv?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&first_air_date.gte=1990-01-01&vote_count.gte=200&without_genres=10763,10767"
                },
                {
                    "name": "Movies",
                    "url": "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&primary_release_date.gte=1990-01-01&vote_count.gte=200"
                }
            ]
        }
    """.trimIndent()

    @Before
    fun setup() {
        every { context.assets } returns assetManager
        every { appDispatcher.io } returns testDispatcher

        repository = TMDBDiscoverCatalogRepositoryImpl(context, appDispatcher)
    }

    @Test
    fun `read parses JSON correctly when valid asset file is provided`() =
        runTest(testDispatcher) {
            val inputStream = ByteArrayInputStream(validJsonResponse.toByteArray())
            every { assetManager.open("test.json") } returns inputStream

            val result = repository.read("test.json")

            expectThat(result) {
                get { networks }.isEqualTo(mockNetworks)
                get { companies }.isEqualTo(mockCompanies)
                get { genres }.isEqualTo(mockGenres)
                get { type }.isEqualTo(mockTypes)
            }
        }

    @Test
    fun `read throws exception when asset file cannot be opened`() =
        runTest(testDispatcher) {
            every { assetManager.open("invalid.json") } throws IOException("File not found")

            try {
                repository.read("invalid.json")
                expectThat("Should have thrown exception").isEqualTo("but didn't")
            } catch (e: IOException) {
                expectThat(e.message).isEqualTo("File not found")
            }
        }

    @Test
    fun `getTvNetworks returns networks list`() =
        runTest(testDispatcher) {
            setupSuccessfulAssetReading()

            val result = repository.getTvNetworks()

            expectThat(result).isEqualTo(mockNetworks)
        }

    @Test
    fun `getMovieCompanies returns companies list`() =
        runTest(testDispatcher) {
            setupSuccessfulAssetReading()

            val result = repository.getMovieCompanies()

            expectThat(result).isEqualTo(mockCompanies)
        }

    @Test
    fun `getTvGenres returns filtered TV genres only`() =
        runTest(testDispatcher) {
            setupSuccessfulAssetReading()

            val result = repository.getTvGenres()

            expectThat(result).hasSize(1)
            result.forEach { genre ->
                expectThat(genre.isForTv).isEqualTo(true)
            }
        }

    @Test
    fun `getMovieGenres returns filtered movie genres only`() =
        runTest(testDispatcher) {
            setupSuccessfulAssetReading()

            val result = repository.getMovieGenres()

            expectThat(result).hasSize(2)
            result.forEach { genre ->
                expectThat(genre.isForMovie).isEqualTo(true)
            }
        }

    @Test
    fun `getGenres returns all genres`() =
        runTest(testDispatcher) {
            setupSuccessfulAssetReading()

            val result = repository.getGenres()

            expectThat(result).hasSize(3)
        }

    @Test
    fun `getTv returns filtered TV content only`() =
        runTest(testDispatcher) {
            setupSuccessfulAssetReading()

            val result = repository.getTv()

            expectThat(result).isEqualTo(listOf(mockTypes[0]))
            result.forEach { content ->
                expectThat(content.isForTv).isEqualTo(true)
            }
        }

    @Test
    fun `getMovies returns filtered movie content only`() =
        runTest(testDispatcher) {
            setupSuccessfulAssetReading()

            val result = repository.getMovies()

            expectThat(result).isEqualTo(listOf(mockTypes[1]))
            result.forEach { content ->
                expectThat(content.isForMovie).isEqualTo(true)
            }
        }

    @Test
    fun `caching works correctly on subsequent calls`() =
        runTest(testDispatcher) {
            val inputStream1 = ByteArrayInputStream(validJsonResponse.toByteArray())
            val inputStream2 = ByteArrayInputStream(validJsonResponse.toByteArray())

            every { assetManager.open("discover_catalogs.json") } returnsMany listOf(inputStream1, inputStream2)

            val result1 = repository.getTvNetworks()
            val result2 = repository.getTvNetworks()

            expectThat(result1).isEqualTo(result2)
            expectThat(result1).isEqualTo(mockNetworks)
        }

    @Test
    fun `multiple concurrent calls use cached data after first successful load`() =
        runTest(testDispatcher) {
            setupSuccessfulAssetReading()

            val results = listOf(
                repository.getTvNetworks(),
                repository.getMovieCompanies(),
                repository.getGenres(),
                repository.getTv(),
                repository.getMovies(),
            )

            expectThat(results[0]).isEqualTo(mockNetworks)
            expectThat(results[1]).isEqualTo(mockCompanies)
            expectThat(results[2]).isEqualTo(mockGenres)
            expectThat(results[3]).isEqualTo(listOf(mockTypes[0]))
            expectThat(results[4]).isEqualTo(listOf(mockTypes[1]))
        }

    @Test
    fun `empty collections handle filtering correctly`() =
        runTest(testDispatcher) {
            val emptyJsonResponse = """
            {
                "networks": [],
                "companies": [],
                "genres": [],
                "type": []
            }
            """.trimIndent()

            val inputStream = ByteArrayInputStream(emptyJsonResponse.toByteArray())
            every { assetManager.open("discover_catalogs.json") } returns inputStream

            val tvGenresResult = repository.getTvGenres()
            val movieGenresResult = repository.getMovieGenres()
            val tvResult = repository.getTv()
            val moviesResult = repository.getMovies()

            expectThat(tvGenresResult).isEmpty()
            expectThat(movieGenresResult).isEmpty()
            expectThat(tvResult).isEmpty()
            expectThat(moviesResult).isEmpty()
        }

    private fun setupSuccessfulAssetReading() {
        val inputStream = ByteArrayInputStream(validJsonResponse.toByteArray())
        every { assetManager.open("discover_catalogs.json") } returns inputStream
    }
}
