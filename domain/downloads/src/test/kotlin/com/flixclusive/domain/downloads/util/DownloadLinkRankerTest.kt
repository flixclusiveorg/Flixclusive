package com.flixclusive.domain.downloads.util

import com.flixclusive.core.datastore.model.user.download.DownloadLinkSelectionMode
import com.flixclusive.core.datastore.model.user.download.DownloadLinkSortDirection
import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.core.network.download.LinkProbeResult
import com.flixclusive.model.provider.link.Stream
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly

class DownloadLinkRankerTest {
    private fun stream(name: String) = Stream(name = name, url = "https://example.com/$name")

    private fun result(
        isReachable: Boolean = true,
        contentLength: Long? = null,
        bytesPerSecond: Long? = null,
    ) = LinkProbeResult(isReachable = isReachable, contentLength = contentLength, bytesPerSecond = bytesPerSecond)

    @Test
    fun `rank should put the preferred quality first regardless of direction`() {
        val stream480p = stream("480p") to result()
        val stream1080p = stream("1080p") to result()
        val stream720p = stream("720p") to result()

        val ranked = DownloadLinkRanker.rank(
            candidates = listOf(stream480p, stream1080p, stream720p),
            mode = DownloadLinkSelectionMode.QUALITY_FIRST,
            direction = DownloadLinkSortDirection.LOWEST_FIRST,
            preferredQuality = PlayerQuality.Quality1080p,
        )

        expectThat(ranked.map { it.first.name }).containsExactly("1080p", "720p", "480p")
    }

    @Test
    fun `rank should prefer stepping down over wrapping up to a higher tier when direction is LOWEST_FIRST`() {
        val stream4k = stream("4k") to result()
        val stream720p = stream("720p") to result()

        val ranked = DownloadLinkRanker.rank(
            candidates = listOf(stream4k, stream720p),
            mode = DownloadLinkSelectionMode.QUALITY_FIRST,
            direction = DownloadLinkSortDirection.LOWEST_FIRST,
            preferredQuality = PlayerQuality.Quality1080p,
        )

        expectThat(ranked.map { it.first.name }).containsExactly("720p", "4k")
    }

    @Test
    fun `rank should prefer stepping up over wrapping down to a lower tier when direction is HIGHEST_FIRST`() {
        val stream4k = stream("4k") to result()
        val stream720p = stream("720p") to result()

        val ranked = DownloadLinkRanker.rank(
            candidates = listOf(stream4k, stream720p),
            mode = DownloadLinkSelectionMode.QUALITY_FIRST,
            direction = DownloadLinkSortDirection.HIGHEST_FIRST,
            preferredQuality = PlayerQuality.Quality1080p,
        )

        expectThat(ranked.map { it.first.name }).containsExactly("4k", "720p")
    }

    @Test
    fun `rank should sort unmatched quality links last regardless of direction`() {
        val streamKnown = stream("1080p") to result()
        val streamUnknown = stream("mystery-link") to result()

        val ranked = DownloadLinkRanker.rank(
            candidates = listOf(streamUnknown, streamKnown),
            mode = DownloadLinkSelectionMode.QUALITY_FIRST,
            direction = DownloadLinkSortDirection.HIGHEST_FIRST,
            preferredQuality = PlayerQuality.Quality1080p,
        )

        expectThat(ranked.map { it.first.name }).containsExactly("1080p", "mystery-link")
    }

    @Test
    fun `rank should sort by speed as a tiebreaker within the same quality`() {
        val slow = stream("1080p-slow") to result(bytesPerSecond = 100)
        val fast = stream("1080p-fast") to result(bytesPerSecond = 500)

        val ranked = DownloadLinkRanker.rank(
            candidates = listOf(slow, fast),
            mode = DownloadLinkSelectionMode.QUALITY_FIRST,
            direction = DownloadLinkSortDirection.HIGHEST_FIRST,
            preferredQuality = PlayerQuality.Quality1080p,
        )

        expectThat(ranked.map { it.first.name }).containsExactly("1080p-fast", "1080p-slow")
    }

    @Test
    fun `rank should sort by largest known size first when direction is HIGHEST_FIRST`() {
        val small = stream("small") to result(contentLength = 100)
        val large = stream("large") to result(contentLength = 900)
        val unknown = stream("unknown") to result(contentLength = null, bytesPerSecond = 50)

        val ranked = DownloadLinkRanker.rank(
            candidates = listOf(small, unknown, large),
            mode = DownloadLinkSelectionMode.SIZE_FIRST,
            direction = DownloadLinkSortDirection.HIGHEST_FIRST,
            preferredQuality = PlayerQuality.Quality1080p,
        )

        expectThat(ranked.map { it.first.name }).containsExactly("large", "small", "unknown")
    }

    @Test
    fun `rank should sort by smallest known size first when direction is LOWEST_FIRST`() {
        val small = stream("small") to result(contentLength = 100)
        val large = stream("large") to result(contentLength = 900)
        val unknown = stream("unknown") to result(contentLength = null, bytesPerSecond = 50)

        val ranked = DownloadLinkRanker.rank(
            candidates = listOf(large, unknown, small),
            mode = DownloadLinkSelectionMode.SIZE_FIRST,
            direction = DownloadLinkSortDirection.LOWEST_FIRST,
            preferredQuality = PlayerQuality.Quality1080p,
        )

        expectThat(ranked.map { it.first.name }).containsExactly("small", "large", "unknown")
    }
}
