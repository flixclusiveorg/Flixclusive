package com.flixclusive.data.downloads.hls

import com.flixclusive.core.datastore.model.user.download.DownloadLinkSortDirection

data class HlsSegmentInfo(
    val url: String,
    val byteRangeOffset: Long,
    val byteRangeLength: Long,
    val encryptionKeyUri: String?,
    val encryptionIv: String?,
)

data class ResolvedHlsPlaylist(
    val segments: List<HlsSegmentInfo>,
)

sealed class HlsResolutionResult {
    data class Success(
        val playlist: ResolvedHlsPlaylist,
    ) : HlsResolutionResult()

    data class Failed(
        val reason: String,
    ) : HlsResolutionResult()
}

interface HlsManifestResolver {
    /**
     * Fetches and parses the manifest at [url] (a multivariant or media playlist), selecting a
     * variant with muxed audio when it's a multivariant playlist, and returns its segment list
     * ready for download. Fails for live (no `#EXT-X-ENDLIST`) manifests since a live stream has
     * no finite end to download.
     *
     * @param direction among usable (muxed-audio, non-trick-play) variants, [DownloadLinkSortDirection.HIGHEST_FIRST]
     * picks the highest resolution/bitrate and [DownloadLinkSortDirection.LOWEST_FIRST] the lowest —
     * an HLS variant's file size tracks its resolution directly, so quality and size are the same
     * single axis here, unlike progressive links where they're probed independently.
     */
    suspend fun resolve(
        url: String,
        headers: Map<String, String>,
        direction: DownloadLinkSortDirection,
    ): HlsResolutionResult
}
