package com.flixclusive.data.tmdb.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.tmdb.TMDBTestDefaults
import com.flixclusive.core.util.log.LogRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull

class TMDBAssetsRepositoryImplTest {
    private lateinit var tmdbApiService: TMDBApiService
    private lateinit var appDispatchers: AppDispatchers
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TMDBAssetsRepositoryImpl

    @get:Rule
    val logRule = LogRule()

    @Before
    fun setup() {
        tmdbApiService = TMDBTestDefaults.createTMDBApiService()
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        repository = TMDBAssetsRepositoryImpl(
            tmdbApiService,
            appDispatchers,
        )
    }

    @Test
    fun `getLogo returns success with real logo URL for popular movie`() =
        runTest(testDispatcher) {
            val mediaType = "movie"
            val id = 550 // Fight Club - known to have logo assets

            val result = repository.getLogo(mediaType, id)

            expectThat(result).isA<Resource.Success<String>>()
            val logoUrl = (result as Resource.Success).data
            expectThat(logoUrl).isNotNull()
            expectThat(logoUrl!!) {
                isNotEmpty()
                contains(".png")
            }
        }

    @Test
    fun `getLogo returns success with real logo URL for popular TV show`() =
        runTest(testDispatcher) {
            val mediaType = "tv"
            val id = 1399 // Game of Thrones - known to have logo assets

            val result = repository.getLogo(mediaType, id)

            expectThat(result).isA<Resource.Success<String>>()
            val logoUrl = (result as Resource.Success).data
            expectThat(logoUrl).isNotNull()
            expectThat(logoUrl!!) {
                isNotEmpty()
                contains(".png")
            }
        }

    @Test
    fun `getPosterWithoutLogo returns success with real poster URL for movie`() =
        runTest(testDispatcher) {
            val mediaType = "movie"
            val id = 27205 // Inception - guaranteed to have poster assets

            val result = repository.getPosterWithoutLogo(mediaType, id)

            expectThat(result).isA<Resource.Success<String>>()
            val posterUrl = (result as Resource.Success).data
            expectThat(posterUrl).isNotNull()
            expectThat(posterUrl!!) {
                isNotEmpty()
                contains("/")
            }
        }

    @Test
    fun `getPosterWithoutLogo returns success with real poster URL for TV show`() =
        runTest(testDispatcher) {
            val mediaType = "tv"
            val id = 1396 // Breaking Bad - guaranteed to have poster assets

            val result = repository.getPosterWithoutLogo(mediaType, id)

            expectThat(result).isA<Resource.Success<String>>()
            val posterUrl = (result as Resource.Success).data
            expectThat(posterUrl).isNotNull()
            expectThat(posterUrl!!).isNotEmpty()
        }

    @Test
    fun `getLogo returns failure for non-existent movie`() =
        runTest(testDispatcher) {
            val mediaType = "movie"
            val id = 999999999

            val result = repository.getLogo(mediaType, id)

            expectThat(result).isA<Resource.Failure>()
        }

    @Test
    fun `getPosterWithoutLogo returns failure for non-existent TV show`() =
        runTest(testDispatcher) {
            val mediaType = "tv"
            val id = 999999999

            val result = repository.getPosterWithoutLogo(mediaType, id)

            expectThat(result).isA<Resource.Failure>()
        }

    @Test
    fun `getLogo with Marvel movie returns logo with PNG extension`() =
        runTest(testDispatcher) {
            val mediaType = "movie"
            val id = 299536 // Avengers: Infinity War - Marvel movie with logo

            val result = repository.getLogo(mediaType, id)

            expectThat(result).isA<Resource.Success<String>>()
            val logoUrl = (result as Resource.Success).data
            expectThat(logoUrl).isNotNull()
            expectThat(logoUrl!!).contains(".png") // Converted from SVG
        }
}
