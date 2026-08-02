package com.flixclusive.data.downloads.hls

import com.flixclusive.core.datastore.model.user.download.DownloadLinkSelectionMode

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
     * @param mode among usable (muxed-audio, non-trick-play) variants, [DownloadLinkSelectionMode.QUALITY_FIRST]
     * picks the highest resolution/bitrate and [DownloadLinkSelectionMode.SIZE_FIRST] the lowest —
     * HLS variant size tracks resolution directly, so there's no separate probed-size axis the way
     * there is for progressive links.
     */
    suspend fun resolve(
        url: String,
        headers: Map<String, String>,
        mode: DownloadLinkSelectionMode,
    ): HlsResolutionResult
}
