package com.flixclusive.data.tmdb.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.testing.tmdb.TMDBTestDefaults
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.model.film.TMDBCollection
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isTrue

class TMDBMovieCollectionRepositoryImplTest {
    private lateinit var tmdbApiService: TMDBApiService
    private lateinit var appDispatchers: AppDispatchers
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TMDBMovieCollectionRepositoryImpl

    @get:Rule
    val logRule = LogRule()

    @Before
    fun setup() {
        tmdbApiService = TMDBTestDefaults.createTMDBApiService()
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)

        repository = TMDBMovieCollectionRepositoryImpl(
            tmdbApiService,
            appDispatchers,
        )
    }

    @Test
    fun `getCollection returns success with real collection data`() =
        runTest(testDispatcher) {
            val collectionId = 263 // The Dark Knight Collection

            val result = repository.getCollection(collectionId)

            expectThat(result).isA<Resource.Success<TMDBCollection>>()
            val collection = (result as Resource.Success).data
            expectThat(collection).isNotNull()
            expectThat(collection!!) {
                get { id }.isEqualTo(collectionId)
                get { collectionName }.isNotEmpty()
                get { overview }.isNotNull()
                get { films }.isNotEmpty()
            }
        }

    @Test
    fun `getCollection returns success for Marvel collection`() =
        runTest(testDispatcher) {
            val collectionId = 131295 // Marvel Cinematic Universe Phase One

            val result = repository.getCollection(collectionId)

            expectThat(result).isA<Resource.Success<TMDBCollection>>()
            val collection = (result as Resource.Success).data
            expectThat(collection).isNotNull()
            expectThat(collection!!) {
                get { id }.isEqualTo(collectionId)
                get { collectionName }.isNotEmpty()
                get { overview }.isNotNull()
                get { films }.isNotEmpty()
            }
        }

    @Test
    fun `getCollection returns failure for non-existent collection`() =
        runTest(testDispatcher) {
            val invalidCollectionId = 999999999

            val result = repository.getCollection(invalidCollectionId)

            expectThat(result).isA<Resource.Failure>()
        }

    @Test
    fun `getCollection returns collection with poster and backdrop paths`() =
        runTest(testDispatcher) {
            val collectionId = 263 // The Dark Knight Collection

            val result = repository.getCollection(collectionId)

            expectThat(result).isA<Resource.Success<TMDBCollection>>()
            val collection = (result as Resource.Success).data
            expectThat(collection).isNotNull()
            expectThat(collection!!) {
                get { posterPath }.isNotNull()
                get { backdropPath }.isNotNull()
            }
        }

    @Test
    fun `getCollection returns collection with multiple films`() =
        runTest(testDispatcher) {
            val collectionId = 119 // The Lord of the Rings Collection

            val result = repository.getCollection(collectionId)

            expectThat(result).isA<Resource.Success<TMDBCollection>>()
            val collection = (result as Resource.Success).data
            expectThat(collection).isNotNull()
            expectThat(collection!!) {
                get { films.size }.isEqualTo(3)
                get { films.all { it.title.isNotEmpty() } }.isTrue()
            }
        }
}
