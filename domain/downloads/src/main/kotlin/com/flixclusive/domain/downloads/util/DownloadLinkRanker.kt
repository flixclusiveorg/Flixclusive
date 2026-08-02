package com.flixclusive.domain.downloads.util

import com.flixclusive.core.datastore.model.user.download.DownloadLinkSelectionMode
import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.core.network.download.LinkProbeResult
import com.flixclusive.model.provider.link.Stream

object DownloadLinkRanker {
    fun rank(
        candidates: List<Pair<Stream, LinkProbeResult>>,
        mode: DownloadLinkSelectionMode,
        preferredQuality: PlayerQuality,
    ): List<Pair<Stream, LinkProbeResult>> {
        val primary: Comparator<Pair<Stream, LinkProbeResult>> =
            when (mode) {
                DownloadLinkSelectionMode.QUALITY_FIRST ->
                    compareBy { (stream, _) -> qualityDistance(stream, preferredQuality) }
                DownloadLinkSelectionMode.SIZE_FIRST ->
                    compareByDescending { (_, result) -> result.contentLength ?: UNKNOWN_RANK }
            }

        val bySpeed = compareByDescending<Pair<Stream, LinkProbeResult>> { (_, result) ->
            result.bytesPerSecond
                ?: UNKNOWN_RANK
        }

        return candidates.sortedWith(primary.then(bySpeed))
    }

    private fun qualityDistance(stream: Stream, preferred: PlayerQuality): Int {
        val text = "${stream.name} ${stream.url}"
        val matchedOrdinal = PlayerQuality.entries.firstOrNull { it.regex.containsMatchIn(text) }?.ordinal

        // Unmatched links rank last; matched ones wrap around the quality list the same way
        // the player's auto-select fallback does, so the fallback order stays consistent.
        return when (matchedOrdinal) {
            null -> PlayerQuality.entries.size
            else -> (matchedOrdinal - preferred.ordinal + PlayerQuality.entries.size) % PlayerQuality.entries.size
        }
    }

    private const val UNKNOWN_RANK = -1L
}
