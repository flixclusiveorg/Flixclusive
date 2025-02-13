package com.flixclusive.data.provider.cache

import app.cash.turbine.test
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

private const val TEST_PROVIDER_ID = "provider123"

class DefaultCachedLinksRepositoryTest {
    private val testKey = CacheKey.create(
        filmId = "test123",
        providerId = TEST_PROVIDER_ID,
        episode = null
    )

    private lateinit var repository: DefaultCachedLinksRepository
    private lateinit var testCachedLinks: CachedLinks
    private lateinit var testStreams: List<Stream>
    private lateinit var testSubtitles: List<Subtitle>

    @Before
    fun setup() {
        testStreams = List(10) {
            Stream(
                name = "Stream #$it",
                url = "$it"
            )
        }

        testSubtitles = List(10) {
            Subtitle(
                language = "Subtitle #$it",
                url = "$it"
            )
        }

        repository = DefaultCachedLinksRepository()
        testCachedLinks = CachedLinks(
            watchId = "watch123",
            providerId = TEST_PROVIDER_ID,
            thumbnail = "thumbnail.jpg",
        )
    }

    @Test
    fun `test observeCache emits when new media link is added`() = runTest {
        repository.observeCache(testKey).test(timeout = 5.seconds) {
            assertEquals(null, awaitItem())

            val newStream = testStreams.random()
            repository.addStream(testKey, newStream)

            val updatedValue = awaitItem()
            assertEquals(1, updatedValue?.streams?.size)
            assert(updatedValue?.streams?.contains(newStream) == true)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test observeCache returns null for non-existent key`() = runTest {
        val nonExistentKey = CacheKey.create("nonexistent", TEST_PROVIDER_ID, null)

        repository.observeCache(nonExistentKey).test(timeout = 5.seconds) {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test observeCache emits updates when cached links are removed`() = runTest {
        val cacheWithLinks = testCachedLinks.copy(streams = testStreams.take(1).toList())
        repository.storeCache(testKey, cacheWithLinks)

        repository.observeCache(testKey).test(timeout = 5.seconds) {
            assertEquals(cacheWithLinks, awaitItem())

            repository.removeCache(testKey)

            assertNull(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test multiple streams can be added and observed`() = runTest {
        repository.storeCache(testKey, testCachedLinks)

        repository.observeCache(testKey).test(timeout = 5.seconds) {
            assertEquals(null, awaitItem())

            val stream1 = testStreams.elementAt(1)
            val stream2 = testStreams.elementAt(2)

            repository.addStream(testKey, stream1)
            val afterFirstStream = awaitItem()
            assertEquals(1, afterFirstStream?.streams?.size)
            assert(afterFirstStream?.streams?.contains(stream1) == true)

            repository.addStream(testKey, stream2)
            val afterSecondStream = awaitItem()
            assertEquals(2, afterSecondStream?.streams?.size)
            assert(afterSecondStream?.streams?.containsAll(setOf(stream1, stream2)) == true)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test getCache returns null if all stream links are expired`() = runTest {
        val newCache = testCachedLinks.copy(
            streams = listOf(
                Stream(
                    name = "Expired stream",
                    url = "",
                    flags = setOf(Flag.Expires(expiresOn = 5))
                )
            )
        )

        repository.storeCache(testKey, newCache)

        val currentCache = repository.getCache(testKey)
        assertNull(currentCache)
    }
}

