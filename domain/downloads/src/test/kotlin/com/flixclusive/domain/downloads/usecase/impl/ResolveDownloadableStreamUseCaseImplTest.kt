package com.flixclusive.domain.downloads.usecase.impl

import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.database.entity.media.DBMedia
import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.database.entity.provider.MediaLinksWithData
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.DataPreferences
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.core.datastore.model.user.download.DownloadLinkSelectionMode
import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.data.downloads.probe.LinkProbe
import com.flixclusive.data.downloads.probe.LinkProbeResult
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.domain.downloads.usecase.RankedDownloadCandidate
import com.flixclusive.model.media.common.MediaType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class ResolveDownloadableStreamUseCaseImplTest {
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var mediaLinksRepository: MediaLinksRepository
    private lateinit var linkProbe: LinkProbe
    private lateinit var useCase: ResolveDownloadableStreamUseCaseImpl

    private val ownerId = "owner-1"
    private val mediaId = "media-1"

    private val media = DBMedia(
        id = mediaId,
        title = "Test Movie",
        providerId = "test-provider",
        adult = false,
        type = MediaType.MOVIE,
        overview = null,
        posterImage = null,
        language = null,
        rating = null,
        backdropImage = null,
        releaseDate = null,
    )

    @Before
    fun setup() {
        dataStoreManager = mockk()
        mediaLinksRepository = mockk()
        linkProbe = mockk()

        every {
            dataStoreManager.getUserPrefsAsFlow(UserPreferences.DATA_PREFS_KEY, DataPreferences::class)
        } returns flowOf(DataPreferences(downloadLinkSelectionMode = DownloadLinkSelectionMode.QUALITY_FIRST))

        every {
            dataStoreManager.getUserPrefsAsFlow(UserPreferences.PLAYER_PREFS_KEY, PlayerPreferences::class)
        } returns flowOf(PlayerPreferences(quality = PlayerQuality.Quality1080p))

        coEvery { mediaLinksRepository.setLinkStatus(any(), any(), any()) } returns Unit

        useCase = ResolveDownloadableStreamUseCaseImpl(dataStoreManager, mediaLinksRepository, linkProbe)
    }

    private fun stream(
        name: String,
        url: String = "https://example.com/$name",
    ) = CachedStream(
        url = url,
        label = name,
        providerId = "test-provider",
        ownerId = ownerId,
        mediaId = mediaId,
    )

    private fun linksWith(vararg streams: CachedStream) =
        listOf(MediaLinksWithData(media = media, streams = streams.toList()))

    @Test
    fun `invoke should return failure when no valid cached streams exist`() =
        runTest {
            coEvery { mediaLinksRepository.getLinks(ownerId, mediaId, null, null) } returns emptyList()

            val result = useCase(ownerId, mediaId, null, null)

            expectThat(result).isA<Async.Failure>()
        }

    @Test
    fun `invoke should filter out third-party gateway and dead cached streams`() =
        runTest {
            val gateway = stream("gateway").copy(isThirdPartyGateway = true)
            val dead = stream("dead").copy(isDead = true)
            coEvery { mediaLinksRepository.getLinks(ownerId, mediaId, null, null) } returns linksWith(gateway, dead)

            val result = useCase(ownerId, mediaId, null, null)

            expectThat(result).isA<Async.Failure>()
            coVerify(exactly = 0) { linkProbe.probe(any(), any()) }
        }

    @Test
    fun `invoke should return the best ranked reachable stream`() =
        runTest {
            val best = stream("1080p")
            val worse = stream("480p")
            coEvery { mediaLinksRepository.getLinks(ownerId, mediaId, null, null) } returns linksWith(worse, best)

            coEvery { linkProbe.probe(best.url, any()) } returns
                LinkProbeResult(isReachable = true, contentLength = null, bytesPerSecond = 500)
            coEvery { linkProbe.probe(worse.url, any()) } returns
                LinkProbeResult(isReachable = true, contentLength = null, bytesPerSecond = 100)

            val result = useCase(ownerId, mediaId, null, null)

            expectThat(result)
                .isA<Async.Success<RankedDownloadCandidate>>()
                .get { data.stream.url }
                .isEqualTo(best.url)
        }

    @Test
    fun `invoke should mark unreachable candidates dead and fall back to the next one`() =
        runTest {
            val unreachable = stream("1080p")
            val reachable = stream("720p")
            coEvery { mediaLinksRepository.getLinks(ownerId, mediaId, null, null) } returns
                linksWith(unreachable, reachable)

            coEvery { linkProbe.probe(unreachable.url, any()) } returns
                LinkProbeResult(isReachable = false, contentLength = null, bytesPerSecond = null)
            coEvery { linkProbe.probe(reachable.url, any()) } returns
                LinkProbeResult(isReachable = true, contentLength = null, bytesPerSecond = 200)

            val result = useCase(ownerId, mediaId, null, null)

            expectThat(result)
                .isA<Async.Success<RankedDownloadCandidate>>()
                .get { data.stream.url }
                .isEqualTo(reachable.url)
            coVerify { mediaLinksRepository.setLinkStatus(unreachable.url, ownerId, isDead = true) }
        }

    @Test
    fun `invoke should mark a probed HLS candidate as such`() =
        runTest {
            val hls = stream("hls")
            coEvery { mediaLinksRepository.getLinks(ownerId, mediaId, null, null) } returns linksWith(hls)
            coEvery { linkProbe.probe(hls.url, any()) } returns
                LinkProbeResult(isReachable = true, contentLength = null, bytesPerSecond = 500, isHls = true)

            val result = useCase(ownerId, mediaId, null, null)

            val success = expectThat(result).isA<Async.Success<RankedDownloadCandidate>>().subject
            expectThat(success.data.isHls).isTrue()
        }

    @Test
    fun `invoke should mark every dead candidate and return failure when nothing reachable remains`() =
        runTest {
            val streams = listOf(stream("1080p"), stream("720p"), stream("480p"))
            coEvery { mediaLinksRepository.getLinks(ownerId, mediaId, null, null) } returns
                linksWith(*streams.toTypedArray())
            streams.forEach { s ->
                coEvery { linkProbe.probe(s.url, any()) } returns
                    LinkProbeResult(isReachable = false, contentLength = null, bytesPerSecond = null)
            }

            val result = useCase(ownerId, mediaId, null, null)

            expectThat(result).isA<Async.Failure>()
            streams.forEach { s ->
                coVerify { mediaLinksRepository.setLinkStatus(s.url, ownerId, isDead = true) }
            }
        }
}
