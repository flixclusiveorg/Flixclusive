package com.flixclusive.data.downloads.hls.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.datastore.model.user.download.DownloadLinkSortDirection
import com.flixclusive.core.testing.dispatcher.DispatcherTestDefaults
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.data.downloads.hls.HlsResolutionResult
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo

/**
 * Instrumented, not a JVM unit test: [HlsManifestResolverImpl] delegates parsing to media3's
 * [androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser], which calls real Android framework
 * classes (android.net.Uri, TextUtils, Base64) internally — those throw/hang under plain JVM unit
 * tests since this project has no Robolectric, matching the existing convention of putting
 * Android-framework-dependent tests (e.g. [com.flixclusive.core.database.DBMigrationTest]) here
 * instead of introducing a new test-only dependency.
 */
@RunWith(AndroidJUnit4::class)
class HlsManifestResolverImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var server: MockWebServer
    private lateinit var resolver: HlsManifestResolverImpl

    @get:Rule
    val logRule = LogRule()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        resolver = HlsManifestResolverImpl(
            client = OkHttpClient(),
            appDispatchers = DispatcherTestDefaults.createTestAppDispatchers(testDispatcher),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val vodMediaPlaylist = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-TARGETDURATION:10
        #EXT-X-MEDIA-SEQUENCE:0
        #EXTINF:10.0,
        segment0.ts
        #EXTINF:10.0,
        segment1.ts
        #EXT-X-ENDLIST
    """.trimIndent()

    private val liveMediaPlaylist = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-TARGETDURATION:10
        #EXT-X-MEDIA-SEQUENCE:0
        #EXTINF:10.0,
        segment0.ts
    """.trimIndent()

    @Test
    fun resolveShouldReturnSegmentListForStandaloneMediaPlaylist() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setBody(vodMediaPlaylist))

            val result = resolver.resolve(
                server.url("/media.m3u8").toString(),
                emptyMap(),
                DownloadLinkSortDirection.HIGHEST_FIRST
            )

            expectThat(result)
                .isA<HlsResolutionResult.Success>()
                .get { playlist.segments }
                .hasSize(2)
        }

    private val fmp4MediaPlaylist = """
        #EXTM3U
        #EXT-X-VERSION:7
        #EXT-X-TARGETDURATION:10
        #EXT-X-MEDIA-SEQUENCE:0
        #EXT-X-MAP:URI="init.mp4"
        #EXTINF:10.0,
        segment0.m4s
        #EXTINF:10.0,
        segment1.m4s
        #EXT-X-ENDLIST
    """.trimIndent()

    private val fmp4MediaPlaylistWithChangingMap = """
        #EXTM3U
        #EXT-X-VERSION:7
        #EXT-X-TARGETDURATION:10
        #EXT-X-MEDIA-SEQUENCE:0
        #EXT-X-MAP:URI="init0.mp4"
        #EXTINF:10.0,
        segment0.m4s
        #EXT-X-MAP:URI="init1.mp4"
        #EXTINF:10.0,
        segment1.m4s
        #EXT-X-ENDLIST
    """.trimIndent()

    @Test
    fun resolveShouldEmitTheInitialisationSegmentOnceAheadOfTheMediaSegments() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setBody(fmp4MediaPlaylist))

            val result = resolver.resolve(
                server.url("/media.m3u8").toString(),
                emptyMap(),
                DownloadLinkSortDirection.HIGHEST_FIRST
            )

            expectThat(result)
                .isA<HlsResolutionResult.Success>()
                .get { playlist.segments.map { it.url.substringAfterLast('/') } }
                .isEqualTo(listOf("init.mp4", "segment0.m4s", "segment1.m4s"))
        }

    @Test
    fun resolveShouldReEmitTheInitialisationSegmentWhenItChangesMidPlaylist() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setBody(fmp4MediaPlaylistWithChangingMap))

            val result = resolver.resolve(
                server.url("/media.m3u8").toString(),
                emptyMap(),
                DownloadLinkSortDirection.HIGHEST_FIRST
            )

            expectThat(result)
                .isA<HlsResolutionResult.Success>()
                .get { playlist.segments.map { it.url.substringAfterLast('/') } }
                .isEqualTo(listOf("init0.mp4", "segment0.m4s", "init1.mp4", "segment1.m4s"))
        }

    @Test
    fun resolveShouldFailForLiveMediaPlaylistWithNoEndlistTag() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setBody(liveMediaPlaylist))

            val result = resolver.resolve(
                server.url("/media.m3u8").toString(),
                emptyMap(),
                DownloadLinkSortDirection.HIGHEST_FIRST
            )

            expectThat(result).isA<HlsResolutionResult.Failed>()
        }

    private val twoVariantMaster = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360,CODECS="avc1.640028,mp4a.40.2"
        low.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720,CODECS="avc1.640028,mp4a.40.2"
        high.m3u8
    """.trimIndent()

    @Test
    fun resolveShouldSelectHighestQualityVariantWhenDirectionIsHighestFirst() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setBody(twoVariantMaster))
            server.enqueue(MockResponse().setBody(vodMediaPlaylist))

            resolver.resolve(
                server.url("/master.m3u8").toString(),
                emptyMap(),
                DownloadLinkSortDirection.HIGHEST_FIRST
            )

            val requestedPaths = listOf(server.takeRequest().path, server.takeRequest().path)
            expectThat(requestedPaths).isEqualTo(listOf("/master.m3u8", "/high.m3u8"))
        }

    @Test
    fun resolveShouldSelectLowestQualityVariantWhenDirectionIsLowestFirst() =
        runTest(testDispatcher) {
            server.enqueue(MockResponse().setBody(twoVariantMaster))
            server.enqueue(MockResponse().setBody(vodMediaPlaylist))

            resolver.resolve(
                server.url("/master.m3u8").toString(),
                emptyMap(),
                DownloadLinkSortDirection.LOWEST_FIRST
            )

            val requestedPaths = listOf(server.takeRequest().path, server.takeRequest().path)
            expectThat(requestedPaths).isEqualTo(listOf("/master.m3u8", "/low.m3u8"))
        }

    @Test
    fun resolveShouldFailWhenEveryVariantRequiresSeparateAudioTrack() =
        runTest(testDispatcher) {
            val master = """
                #EXTM3U
                #EXT-X-VERSION:3
                #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud1",NAME="English",URI="audio.m3u8",DEFAULT=YES
                #EXT-X-STREAM-INF:BANDWIDTH=1280000,RESOLUTION=640x360,CODECS="avc1.640028",AUDIO="aud1"
                video.m3u8
            """.trimIndent()

            server.enqueue(MockResponse().setBody(master))

            val result = resolver.resolve(
                server.url("/master.m3u8").toString(),
                emptyMap(),
                DownloadLinkSortDirection.HIGHEST_FIRST
            )

            expectThat(result).isA<HlsResolutionResult.Failed>()
        }

    @Test
    fun resolveShouldCarryEncryptionMetadataForEncryptedSegments() =
        runTest(testDispatcher) {
            val encryptedPlaylist = """
                #EXTM3U
                #EXT-X-VERSION:3
                #EXT-X-TARGETDURATION:10
                #EXT-X-KEY:METHOD=AES-128,URI="key",IV=0x00000000000000000000000000000001
                #EXTINF:10.0,
                segment0.ts
                #EXT-X-ENDLIST
            """.trimIndent()

            server.enqueue(MockResponse().setBody(encryptedPlaylist))

            val result = resolver.resolve(
                server.url("/media.m3u8").toString(),
                emptyMap(),
                DownloadLinkSortDirection.HIGHEST_FIRST
            )

            expectThat(result)
                .isA<HlsResolutionResult.Success>()
                .get { playlist.segments.first().encryptionKeyUri }
                .isEqualTo(server.url("/key").toString())
        }
}
