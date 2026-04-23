package com.flixclusive.data.provider.repository.impl

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.data.provider.repository.MediaLinks
import com.flixclusive.data.provider.repository.MediaLinksCacheKey
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.link.Flag
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.util.Date

class MediaLinksRepositoryImplTest {
    private lateinit var repository: MediaLinksRepositoryImpl
    private lateinit var appDispatchers: AppDispatchers
    private val testDispatcher = StandardTestDispatcher()

    private val testMediaLinksCacheKey = MediaLinksCacheKey.create(
        filmId = "film123",
        providerId = "provider1",
        episode = null,
    )

    private val testMediaLinks = MediaLinks(
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
        appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher)
        repository = MediaLinksRepositoryImpl(appDispatchers)
    }

    @Test
    fun `insertLinks should store cache and update state flow`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            repository.insertLinks(testMediaLinksCacheKey, testMediaLinks)

            turbineScope {
                val caches = repository.caches.testIn(this)
                val currentCache = repository.observeLinks(testMediaLinksCacheKey).testIn(this)

                with(caches) {
                    val emission = awaitItem()
                    expectThat(emission).hasSize(1)
                    expectThat(emission[testMediaLinksCacheKey]).isEqualTo(testMediaLinks)
                    cancelAndIgnoreRemainingEvents()
                }

                with(currentCache) {
                    val emission = awaitItem()
                    expectThat(emission).isEqualTo(testMediaLinks)
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

    @Test
    fun `addStream should append stream to existing cache`() =
        runTest(testDispatcher) {
            repository.insertLinks(testMediaLinksCacheKey, testMediaLinks)

            val newStream = Stream(
                url = "http://example.com/stream2",
                name = "Stream 2",
            )

            repository.addStream(testMediaLinksCacheKey, newStream)

            val cache = repository.getLinks(testMediaLinksCacheKey)
            expectThat(cache).isEqualTo(
                testMediaLinks.copy(
                    streams = testMediaLinks.streams + newStream,
                ),
            )
        }

    @Test
    fun `addStream should not add duplicate stream`() =
        runTest(testDispatcher) {
            repository.insertLinks(testMediaLinksCacheKey, testMediaLinks)

            val duplicateStream = testMediaLinks.streams.first()
            repository.addStream(testMediaLinksCacheKey, duplicateStream)

            val cache = repository.getLinks(testMediaLinksCacheKey)
            expectThat(cache!!.streams).hasSize(1)
        }

    @Test
    fun `addSubtitle should append subtitle to existing cache`() =
        runTest(testDispatcher) {
            repository.insertLinks(testMediaLinksCacheKey, testMediaLinks)

            val newSubtitle = Subtitle(
                url = "http://example.com/subtitle2",
                language = "es",
            )

            repository.addSubtitle(testMediaLinksCacheKey, newSubtitle)

            val cache = repository.getLinks(testMediaLinksCacheKey)
            expectThat(cache).isEqualTo(
                testMediaLinks.copy(
                    subtitles = testMediaLinks.subtitles + newSubtitle,
                ),
            )
        }

    @Test
    fun `addSubtitle should not add duplicate subtitle`() =
        runTest(testDispatcher) {
            repository.insertLinks(testMediaLinksCacheKey, testMediaLinks)

            val duplicateSubtitle = testMediaLinks.subtitles.first()
            repository.addSubtitle(testMediaLinksCacheKey, duplicateSubtitle)

            val cache = repository.getLinks(testMediaLinksCacheKey)
            expectThat(cache!!.subtitles).hasSize(1)
        }

    @Test
    fun `removeCache should remove cache from both keys`() =
        runTest(testDispatcher) {
            repository.insertLinks(testMediaLinksCacheKey, testMediaLinks)
            repository.removeCache(testMediaLinksCacheKey)

            repository.caches.test {
                val emission = awaitItem()
                expectThat(emission).hasSize(0)
            }
        }

    @Test
    fun `getCache should return null for non-existent key`() =
        runTest(testDispatcher) {
            val result = repository.getLinks(testMediaLinksCacheKey)
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

            val cacheWithExpiredStreams = testMediaLinks.copy(
                streams = listOf(expiredStream, validStream),
            )

            repository.insertLinks(testMediaLinksCacheKey, cacheWithExpiredStreams)

            val result = repository.getLinks(testMediaLinksCacheKey)
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

            val cacheWithOnlyExpiredStreams = testMediaLinks.copy(
                streams = listOf(expiredStream),
            )

            repository.insertLinks(testMediaLinksCacheKey, cacheWithOnlyExpiredStreams)

            val result = repository.getLinks(testMediaLinksCacheKey)
            expectThat(result).isNull()
        }

    @Test
    fun `observeLinks should return null if cache has no valid streams`() =
        runTest(testDispatcher) {
            val defaultCache = MediaLinks(watchId = "default", providerId = "provider1")
            repository.insertLinks(testMediaLinksCacheKey, defaultCache)

            repository.currentObservable.test {
                val emission = awaitItem()
                expectThat(emission).isNull()
            }
        }

    @Test
    fun `observeLinks should emit cached links when valid streams exist`() =
        runTest(testDispatcher) {
            repository.insertLinks(testMediaLinksCacheKey, testMediaLinks)

            repository.observeLinks(testMediaLinksCacheKey).test {
                val emission = awaitItem()
                expectThat(emission).isEqualTo(testMediaLinks)
            }
        }

    @Test
    fun `observeLinks should emit null when all streams are expired`() =
        runTest(testDispatcher) {
            val expiredStream = Stream(
                url = "http://example.com/expired",
                name = "Expired Stream",
                flags = setOf(Flag.Expires(expiresOn = 0)),
            )

            val cacheWithExpiredStreams = testMediaLinks.copy(
                streams = listOf(expiredStream),
            )

            repository.insertLinks(testMediaLinksCacheKey, cacheWithExpiredStreams)

            repository.currentObservable.test {
                val emission = awaitItem()
                expectThat(emission).isNull()
            }
        }

    @Test
    fun `clear should remove all caches`() =
        runTest(testDispatcher) {
            repository.insertLinks(testMediaLinksCacheKey, testMediaLinks)
            repository.clear()

            repository.caches.test {
                val emission = awaitItem()
                expectThat(emission).hasSize(0)
            }
        }

    @Test
    fun `insertLinks should store on both observable map and currentCache flow`() =
        runTest(testDispatcher) {
            val episode = Episode(
                season = 1,
                number = 1,
                airDate = Date()
            )
            val keyWithEpisode = MediaLinksCacheKey.create(
                filmId = "film123",
                providerId = "provider1",
                episode = episode,
            )

            repository.insertLinks(keyWithEpisode, testMediaLinks)
            repository.setCurrentObservable(keyWithEpisode)

            expectThat(repository.getLinks(keyWithEpisode)).isEqualTo(testMediaLinks)
            repository.currentObservable.test {
                skipItems(1)
                expectThat(awaitItem()).isEqualTo(testMediaLinks)
            }
        }
}
