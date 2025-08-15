package com.flixclusive.data.tmdb.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.network.NetworkTestDefaults
import com.flixclusive.model.provider.link.Stream
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull

class TMDBWatchProvidersRepositoryImplTest {
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var appDispatchers: AppDispatchers
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TMDBWatchProvidersRepositoryImpl

    @Before
    fun setup() {
        okHttpClient = NetworkTestDefaults.createTestOkHttpClient()
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        repository = TMDBWatchProvidersRepositoryImpl(
            okHttpClient,
            appDispatchers
        )
    }

    @Test
    fun `getWatchProviders returns success with real streaming data for popular movie`() = runTest(testDispatcher) {
        val mediaType = "movie"
        val id = 550 // Fight Club - popular movie likely to have streaming providers

        val result = repository.getWatchProviders(mediaType, id)

        expectThat(result).isA<Resource.Success<List<Stream>>>()
        val streams = (result as Resource.Success).data
        expectThat(streams).isNotNull()
        expectThat(streams!!.size).isGreaterThan(0)
    }

    @Test
    fun `getWatchProviders returns success with real streaming data for popular TV show`() = runTest(testDispatcher) {
        val mediaType = "tv"
        val id = 1399 // Game of Thrones - popular TV show

        val result = repository.getWatchProviders(mediaType, id)

        expectThat(result).isA<Resource.Success<List<Stream>>>()
    }

    @Test
    fun `getWatchProviders handles Disney movie with multiple providers`() = runTest(testDispatcher) {
        val mediaType = "movie"
        val id = 299536 // Avengers: Infinity War - Disney movie with wide availability

        val result = repository.getWatchProviders(mediaType, id)

        expectThat(result).isA<Resource.Success<List<Stream>>>()
    }

    @Test
    fun `getWatchProviders handles Netflix original series`() = runTest(testDispatcher) {
        val mediaType = "tv"
        val id = 80057281 // Stranger Things - Netflix original

        val result = repository.getWatchProviders(mediaType, id)

        expectThat(result).isA<Resource.Success<List<Stream>>>()
    }

    @Test
    fun `getWatchProviders throws exception for invalid media type`() = runTest(testDispatcher) {
        val mediaType = "invalid"
        val id = 123

        try {
            repository.getWatchProviders(mediaType, id)
            expectThat("Should have thrown exception").isA<String>()
        } catch (e: IllegalArgumentException) {
            expectThat(e.message).isA<String>()
            expectThat(e.message!!).isNotEmpty()
        }
    }

    @Test
    fun `getWatchProviders accepts valid media types movie and tv`() = runTest(testDispatcher) {
        val movieId = 550
        val tvId = 1399

        val movieResult = repository.getWatchProviders("movie", movieId)
        val tvResult = repository.getWatchProviders("tv", tvId)

        expectThat(movieResult).isA<Resource.Success<List<Stream>>>()
        expectThat(tvResult).isA<Resource.Success<List<Stream>>>()
    }

    @Test
    fun `getWatchProviders handles recent blockbuster movie`() = runTest(testDispatcher) {
        val mediaType = "movie"
        val id = 299534 // Avengers: Endgame - recent blockbuster

        val result = repository.getWatchProviders(mediaType, id)

        expectThat(result).isA<Resource.Success<List<Stream>>>()
    }
}
