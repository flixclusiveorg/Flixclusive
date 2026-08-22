package com.flixclusive.data.downloads.hls.impl

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.UriUtil
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.datastore.model.user.download.DownloadLinkSortDirection
import com.flixclusive.data.downloads.hls.HlsManifestResolver
import com.flixclusive.data.downloads.hls.HlsResolutionResult
import com.flixclusive.data.downloads.hls.HlsSegmentInfo
import com.flixclusive.data.downloads.hls.ResolvedHlsPlaylist
import kotlinx.coroutines.withContext
import com.flixclusive.data.downloads.di.DownloadHttpClient
import com.flixclusive.data.downloads.util.okRequest
import okhttp3.OkHttpClient
import java.io.IOException
import javax.inject.Inject

internal class HlsManifestResolverImpl @Inject constructor(
    @param:DownloadHttpClient private val client: OkHttpClient,
    private val appDispatchers: AppDispatchers,
) : HlsManifestResolver {
    override suspend fun resolve(
        url: String,
        headers: Map<String, String>,
        direction: DownloadLinkSortDirection,
    ): HlsResolutionResult =
        withContext(appDispatchers.io) {
            try {
                resolveInternal(url, headers, direction)
            } catch (e: Throwable) {
                HlsResolutionResult.Failed(e.message ?: "Failed to resolve HLS playlist")
            }
        }

    @OptIn(UnstableApi::class)
    private fun resolveInternal(
        url: String,
        headers: Map<String, String>,
        direction: DownloadLinkSortDirection,
    ): HlsResolutionResult {
        val rootPlaylist = fetchAndParse(url, headers, HlsPlaylistParser())

        var mediaPlaylistUrl = url
        val mediaPlaylist = when (rootPlaylist) {
            is HlsMediaPlaylist -> rootPlaylist
            is HlsMultivariantPlaylist -> {
                val variant = selectVariant(rootPlaylist, direction)
                    ?: return HlsResolutionResult.Failed("No playable HLS variant with muxed audio was found")
                mediaPlaylistUrl = variant.url.toString()

                val variantPlaylist = fetchAndParse(
                    mediaPlaylistUrl,
                    headers,
                    HlsPlaylistParser(rootPlaylist, null)
                )

                variantPlaylist as? HlsMediaPlaylist
                    ?: return HlsResolutionResult.Failed("HLS variant did not resolve to a media playlist")
            }
            else -> return HlsResolutionResult.Failed("Unsupported HLS playlist type")
        }

        if (!mediaPlaylist.hasEndTag) {
            return HlsResolutionResult.Failed("Live HLS streams are not supported for download")
        }

        if (mediaPlaylist.segments.isEmpty()) {
            return HlsResolutionResult.Failed("HLS playlist has no segments")
        }

        // Media3's parser leaves segment/key URIs exactly as written in the manifest — resolution
        // against the enclosing playlist's URL (which ExoPlayer does at a different layer during
        // playback) is left to us here.
        val segments = buildList {
            var currentInit: HlsMediaPlaylist.Segment? = null
            mediaPlaylist.segments.forEach { segment ->
                val init = segment.initializationSegment
                if (init != null && !init.isSameResourceAs(currentInit)) {
                    add(init.toSegmentInfo(mediaPlaylistUrl))
                    currentInit = init
                }

                add(segment.toSegmentInfo(mediaPlaylistUrl))
            }
        }

        return HlsResolutionResult.Success(ResolvedHlsPlaylist(segments))
    }

    @OptIn(UnstableApi::class)
    private fun HlsMediaPlaylist.Segment.isSameResourceAs(other: HlsMediaPlaylist.Segment?): Boolean =
        other != null &&
            url == other.url &&
            byteRangeOffset == other.byteRangeOffset &&
            byteRangeLength == other.byteRangeLength

    @OptIn(UnstableApi::class)
    private fun HlsMediaPlaylist.Segment.toSegmentInfo(playlistUrl: String) =
        HlsSegmentInfo(
            url = UriUtil.resolve(playlistUrl, url),
            byteRangeOffset = byteRangeOffset,
            byteRangeLength = byteRangeLength,
            encryptionKeyUri = fullSegmentEncryptionKeyUri?.let { UriUtil.resolve(playlistUrl, it) },
            encryptionIv = encryptionIV,
        )

    /**
     * Mirrors CS3's `isPlayableStandalone`: a variant is safe to download standalone when it
     * either declares no separate audio group (so its own segments must contain the audio per the
     * HLS spec), or its audio group's rendition has no URI of its own (meaning that audio is
     * embedded in the variant's segments too) — and it isn't a trick-play (I-frame-only) variant.
     * There's no way to fully verify muxing without inspecting the TS segments themselves.
     *
     * Among those usable variants, [DownloadLinkSortDirection.HIGHEST_FIRST] picks the highest
     * resolution/bitrate and [DownloadLinkSortDirection.LOWEST_FIRST] the lowest — a variant's file
     * size tracks its resolution directly, so quality and size are the same single axis here
     * (unlike progressive links, where they're independent and there's a real "closest to the
     * player's preferred quality" concept to rank by — HLS variants have no such per-tier label to
     * match against, only a raw resolution/bitrate figure).
     */
    private fun selectVariant(
        playlist: HlsMultivariantPlaylist,
        direction: DownloadLinkSortDirection,
    ): HlsMultivariantPlaylist.Variant? {
        val usable = playlist.variants.filter { variant ->
            val mustContainAudio = variant.audioGroupId == null ||
                playlist.audios.firstOrNull { it.groupId == variant.audioGroupId }?.url == null
            val isTrickPlay = (variant.format.roleFlags and C.ROLE_FLAG_TRICK_PLAY) != 0
            mustContainAudio && !isTrickPlay
        }

        val bySize = compareBy<HlsMultivariantPlaylist.Variant> { variant ->
            (variant.format.width.toLong() * variant.format.height) * 1000L + variant.format.averageBitrate
        }

        return when (direction) {
            DownloadLinkSortDirection.HIGHEST_FIRST -> usable.maxWithOrNull(bySize)
            DownloadLinkSortDirection.LOWEST_FIRST -> usable.minWithOrNull(bySize)
        }
    }

    private fun fetchAndParse(
        url: String,
        headers: Map<String, String>,
        parser: HlsPlaylistParser,
    ): HlsPlaylist {
        val request = okRequest(url, headers)

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch HLS playlist: ${response.code}")
            return response.body.byteStream().use { stream -> parser.parse(Uri.parse(url), stream) }
        }
    }
}
