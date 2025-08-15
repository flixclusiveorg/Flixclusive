package com.flixclusive.data.provider.repository.impl

import app.cash.turbine.test
import com.flixclusive.data.provider.repository.CacheKey
import com.flixclusive.data.provider.repository.CachedLinks
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class CachedLinksRepositoryImplTest {
    private lateinit var repository: CachedLinksRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    private val testCacheKey = CacheKey.create(
        filmId = "film123",
        providerId = "provider1",
        episode = null,
    )

    private val testCachedLinks = CachedLinks(
        watchId = "watch123",
        providerId = "provider1",
        streams = listOf(
            Stream(
                url = "http://example.com/stream1",
                name = "Stream 1",
            ),
        ),
        subtitles = listOf(
            Subtitle(
                url = "http://example.com/subtitle1",
                language = "en",
            ),
        ),
    )

    @Before
    fun setup() {
        repository = CachedLinksRepositoryImpl()
    }

    @Test
    fun `storeCache should store cache and update state flow`() =
        runTest(testDispatcher) {
            repository.storeCache(testCacheKey, testCachedLinks)

            repository.caches.test {
                val emission = awaitItem()
                expectThat(emission).hasSize(2)
                expectThat(emission[testCacheKey]).isEqualTo(testCachedLinks)
                expectThat(emission[testCacheKey.getFilmOnlyKey()]).isEqualTo(testCachedLinks)
            }
        }

    @Test
    fun `addStream should append stream to existing cache`() =
        runTest(testDispatcher) {
            repository.storeCache(testCacheKey, testCachedLinks)

            val newStream = Stream(
                url = "http://example.com/stream2",
                name = "Stream 2",
            )

            repository.addStream(testCacheKey, newStream)

            val cache = repository.getCache(testCacheKey)
            expectThat(cache).isEqualTo(
                testCachedLinks.copy(
                    streams = testCachedLinks.streams + newStream,
                ),
            )
        }

    @Test
    fun `addStream should not add duplicate stream`() =
        runTest(testDispatcher) {
            repository.storeCache(testCacheKey, testCachedLinks)

            val duplicateStream = testCachedLinks.streams.first()
            repository.addStream(testCacheKey, duplicateStream)

            val cache = repository.getCache(testCacheKey)
            expectThat(cache!!.streams).hasSize(1)
        }

    @Test
    fun `addSubtitle should append subtitle to existing cache`() =
        runTest(testDispatcher) {
            repository.storeCache(testCacheKey, testCachedLinks)

            val newSubtitle = Subtitle(
                url = "http://example.com/subtitle2",
                language = "es",
            )

            repository.addSubtitle(testCacheKey, newSubtitle)

            val cache = repository.getCache(testCacheKey)
            expectThat(cache).isEqualTo(
                testCachedLinks.copy(
                    subtitles = testCachedLinks.subtitles + newSubtitle,
                ),
            )
        }

    @Test
    fun `addSubtitle should not add duplicate subtitle`() =
        runTest(testDispatcher) {
            repository.storeCache(testCacheKey, testCachedLinks)

            val duplicateSubtitle = testCachedLinks.subtitles.first()
            repository.addSubtitle(testCacheKey, duplicateSubtitle)

            val cache = repository.getCache(testCacheKey)
            expectThat(cache!!.subtitles).hasSize(1)
        }

    @Test
    fun `removeCache should remove cache from both keys`() =
        runTest(testDispatcher) {
            repository.storeCache(testCacheKey, testCachedLinks)
            repository.removeCache(testCacheKey)

            repository.caches.test {
                val emission = awaitItem()
                expectThat(emission).hasSize(0)
            }
        }

    @Test
    fun `getCache should return null for non-existent key`() =
        runTest(testDispatcher) {
            val result = repository.getCache(testCacheKey)
            expectThat(result).isNull()
        }

    @Test
    fun `getCache should filter out expired streams`() =
        runTest(testDispatcher) {
            val expiredStream = Stream(
                url = "http://example.com/expired",
                name = "Expired Stream",
                flags = setOf(Flag.Expires(expiresOn = 0)),
            )

            val validStream = Stream(
                url = "http://example.com/valid",
                name = "Valid Stream",
            )

            val cacheWithExpiredStreams = testCachedLinks.copy(
                streams = listOf(expiredStream, validStream),
            )

            repository.storeCache(testCacheKey, cacheWithExpiredStreams)

            val result = repository.getCache(testCacheKey)
            expectThat(result!!.streams).hasSize(1)
            expectThat(result.streams.first()).isEqualTo(validStream)
        }

    @Test
    fun `getCache should return null when all streams are expired`() =
        runTest(testDispatcher) {
            val expiredStream = Stream(
                url = "http://example.com/expired",
                name = "Expired Stream",
                flags = setOf(Flag.Expires(expiresOn = 0)),
            )

            val cacheWithOnlyExpiredStreams = testCachedLinks.copy(
                streams = listOf(expiredStream),
            )

            repository.storeCache(testCacheKey, cacheWithOnlyExpiredStreams)

            val result = repository.getCache(testCacheKey)
            expectThat(result).isNull()
        }

    @Test
    fun `observeCache should return null if cache has no valid streams`() =
        runTest(testDispatcher) {
            val defaultCache = CachedLinks(watchId = "default")

            repository.observeCache(testCacheKey, defaultCache).test {
                val emission = awaitItem()
                expectThat(emission).isNull()
            }
        }

    @Test
    fun `observeCache should emit cached links when valid streams exist`() =
        runTest(testDispatcher) {
            repository.storeCache(testCacheKey, testCachedLinks)

            repository.observeCache(testCacheKey).test {
                val emission = awaitItem()
                expectThat(emission).isEqualTo(testCachedLinks)
            }
        }

    @Test
    fun `observeCache should emit null when all streams are expired`() =
        runTest(testDispatcher) {
            val expiredStream = Stream(
                url = "http://example.com/expired",
                name = "Expired Stream",
                flags = setOf(Flag.Expires(expiresOn = 0)),
            )

            val cacheWithExpiredStreams = testCachedLinks.copy(
                streams = listOf(expiredStream),
            )

            repository.storeCache(testCacheKey, cacheWithExpiredStreams)

            repository.observeCache(testCacheKey).test {
                val emission = awaitItem()
                expectThat(emission).isNull()
            }
        }

    @Test
    fun `clear should remove all caches`() =
        runTest(testDispatcher) {
            repository.storeCache(testCacheKey, testCachedLinks)
            repository.clear()

            repository.caches.test {
                val emission = awaitItem()
                expectThat(emission).hasSize(0)
            }
        }

    @Test
    fun `storeCache should store for both specific and film-only keys`() =
        runTest(testDispatcher) {
            val episode = Episode(season = 1, number = 1)
            val keyWithEpisode = CacheKey.create(
                filmId = "film123",
                providerId = "provider1",
                episode = episode,
            )

            repository.storeCache(keyWithEpisode, testCachedLinks)

            val filmOnlyKey = keyWithEpisode.getFilmOnlyKey()

            expectThat(repository.getCache(keyWithEpisode)).isEqualTo(testCachedLinks)
            expectThat(repository.getCache(filmOnlyKey)).isEqualTo(testCachedLinks)
        }
}
