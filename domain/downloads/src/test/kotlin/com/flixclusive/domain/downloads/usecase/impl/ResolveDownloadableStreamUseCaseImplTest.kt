package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.datastore.model.user.download.DownloadLinkSelectionMode
import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.core.network.download.LinkProbe
import com.flixclusive.core.network.download.LinkProbeResult
import com.flixclusive.domain.downloads.usecase.ResolvedDownloadableStream
import com.flixclusive.model.provider.link.Stream
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class ResolveDownloadableStreamUseCaseImplTest {
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var linkProbe: LinkProbe
    private lateinit var useCase: ResolveDownloadableStreamUseCaseImpl

    @Before
    fun setup() {
        dataStoreManager = mockk()
        linkProbe = mockk()

        every {
            dataStoreManager.getUserPrefsAsFlow(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
        } returns flowOf(DataPreferences(downloadLinkSelectionMode = DownloadLinkSelectionMode.QUALITY_FIRST))

        every {
            dataStoreManager.getUserPrefsAsFlow(UserPreferences.PLAYER_PREFS_KEY, PlayerPreferences::class)
        } returns flowOf(PlayerPreferences(quality = PlayerQuality.Quality1080p))

        useCase = ResolveDownloadableStreamUseCaseImpl(dataStoreManager, linkProbe)
    }

    private fun stream(name: String) = Stream(name = name, url = "https://example.com/$name")

    @Test
    fun `invoke should return failure when streams list is empty`() =
        runTest {
            val result = useCase(emptyList())

            expectThat(result).isA<Async.Failure>()
        }

    @Test
    fun `invoke should return the best ranked reachable stream as the primary`() =
        runTest {
            val best = stream("1080p")
            val worse = stream("480p")

            coEvery { linkProbe.probe(best.url, any()) } returns
                LinkProbeResult(isReachable = true, contentLength = null, bytesPerSecond = 500)
            coEvery { linkProbe.probe(worse.url, any()) } returns
                LinkProbeResult(isReachable = true, contentLength = null, bytesPerSecond = 100)

            val result = useCase(listOf(worse, best))

            expectThat(result).isA<Async.Success<ResolvedDownloadableStream>>().get { data.primary }.isEqualTo(best)
        }

    @Test
    fun `invoke should fall back to the next candidate when the top one is unreachable`() =
        runTest {
            val unreachable = stream("1080p")
            val reachable = stream("720p")

            coEvery { linkProbe.probe(unreachable.url, any()) } returns
                LinkProbeResult(isReachable = false, contentLength = null, bytesPerSecond = null)
            coEvery { linkProbe.probe(reachable.url, any()) } returns
                LinkProbeResult(isReachable = true, contentLength = null, bytesPerSecond = 200)

            val result = useCase(listOf(unreachable, reachable))

            expectThat(result)
                .isA<Async.Success<ResolvedDownloadableStream>>()
                .get { data.primary }
                .isEqualTo(reachable)
        }

    @Test
    fun `invoke should return the remaining ranked candidates as fallbacks`() =
        runTest {
            val best = stream("1080p")
            val second = stream("720p")
            val third = stream("480p")

            coEvery { linkProbe.probe(best.url, any()) } returns
                LinkProbeResult(isReachable = true, contentLength = null, bytesPerSecond = 500)
            coEvery { linkProbe.probe(second.url, any()) } returns
                LinkProbeResult(isReachable = true, contentLength = null, bytesPerSecond = 300)
            coEvery { linkProbe.probe(third.url, any()) } returns
                LinkProbeResult(isReachable = true, contentLength = null, bytesPerSecond = 100)

            val result = useCase(listOf(third, second, best))

            expectThat(result)
                .isA<Async.Success<ResolvedDownloadableStream>>()
                .get { data.fallbacks }
                .isEqualTo(listOf(second, third))
        }

    @Test
    fun `invoke should return failure after exhausting the fallback attempt limit`() =
        runTest {
            val streams = listOf(stream("1080p"), stream("720p"), stream("480p"), stream("360p"))
            streams.forEach { s ->
                coEvery { linkProbe.probe(s.url, any()) } returns
                    LinkProbeResult(isReachable = false, contentLength = null, bytesPerSecond = null)
            }

            val result = useCase(streams)

            expectThat(result).isA<Async.Failure>()
        }
}
