package com.flixclusive.domain.downloads.util

import com.flixclusive.core.datastore.model.user.download.DownloadLinkSelectionMode
import com.flixclusive.core.datastore.model.user.download.DownloadLinkSortDirection
import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.data.downloads.probe.LinkProbeResult
import com.flixclusive.model.provider.link.Stream

object DownloadLinkRanker {
    fun rank(
        candidates: List<Pair<Stream, LinkProbeResult>>,
        mode: DownloadLinkSelectionMode,
        direction: DownloadLinkSortDirection,
        preferredQuality: PlayerQuality,
    ): List<Pair<Stream, LinkProbeResult>> {
        val primary: Comparator<Pair<Stream, LinkProbeResult>> =
            when (mode) {
                DownloadLinkSelectionMode.QUALITY_FIRST ->
                    compareBy { (stream, _) -> qualityDistance(stream, preferredQuality, direction) }
                DownloadLinkSelectionMode.SIZE_FIRST -> sizeComparator(direction)
            }

        val bySpeed = compareByDescending<Pair<Stream, LinkProbeResult>> { (_, result) ->
            result.bytesPerSecond
                ?: UNKNOWN_SPEED_RANK
        }

        return candidates.sortedWith(byDirectFirst.then(primary).then(bySpeed))
    }

    private val byDirectFirst = compareBy<Pair<Stream, LinkProbeResult>> { (_, result) ->
        if (result.isHls) 1 else 0
    }

    /**
     * Ranks by distance from [preferred] first (so an exact/near match always wins regardless of
     * [direction]); [direction] only decides which way ties/wraps break once nothing at that exact
     * tier is available — step towards a higher quality tier first ([DownloadLinkSortDirection.HIGHEST_FIRST])
     * or a lower one first ([DownloadLinkSortDirection.LOWEST_FIRST], matching the player's own
     * auto-select fallback).
     */
    private fun qualityDistance(
        stream: Stream,
        preferred: PlayerQuality,
        direction: DownloadLinkSortDirection,
    ): Int {
        val text = "${stream.name} ${stream.url}"
        val matchedOrdinal = PlayerQuality.entries.firstOrNull { it.regex.containsMatchIn(text) }?.ordinal
            // Unmatched links rank last regardless of direction.
            ?: return PlayerQuality.entries.size

        val size = PlayerQuality.entries.size
        // PlayerQuality is declared highest-to-lowest, so increasing ordinal means lower quality.
        // This is the "step down" distance; walking the other way for HIGHEST_FIRST is just its
        // complement around the same circular list.
        val towardsLowerDistance = (matchedOrdinal - preferred.ordinal + size) % size

        return when (direction) {
            DownloadLinkSortDirection.LOWEST_FIRST -> towardsLowerDistance
            DownloadLinkSortDirection.HIGHEST_FIRST -> (size - towardsLowerDistance) % size
        }
    }

    /**
     * Unknown-size candidates always rank last: [Long.MIN_VALUE] sorts last under
     * [compareByDescending] (HIGHEST_FIRST) and [Long.MAX_VALUE] sorts last under an ascending
     * [compareBy] (LOWEST_FIRST) — a single shared "unknown rank" wouldn't work for both directions.
     */
    private fun sizeComparator(direction: DownloadLinkSortDirection): Comparator<Pair<Stream, LinkProbeResult>> =
        when (direction) {
            DownloadLinkSortDirection.HIGHEST_FIRST ->
                compareByDescending { (_, result) -> result.contentLength ?: Long.MIN_VALUE }
            DownloadLinkSortDirection.LOWEST_FIRST ->
                compareBy { (_, result) -> result.contentLength ?: Long.MAX_VALUE }
        }

    private const val UNKNOWN_SPEED_RANK = -1L
}
