package com.flixclusive.data.downloads.hls

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
     * Fetches and parses the manifest at [url] (a multivariant or media playlist), selecting the
     * best variant with muxed audio when it's a multivariant playlist, and returns its segment
     * list ready for download. Fails for live (no `#EXT-X-ENDLIST`) manifests since a live stream
     * has no finite end to download.
     */
    suspend fun resolve(
        url: String,
        headers: Map<String, String>,
    ): HlsResolutionResult
}
