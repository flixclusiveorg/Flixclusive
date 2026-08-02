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
import com.flixclusive.core.datastore.model.user.download.DownloadLinkSelectionMode
import com.flixclusive.data.downloads.hls.HlsManifestResolver
import com.flixclusive.data.downloads.hls.HlsResolutionResult
import com.flixclusive.data.downloads.hls.HlsSegmentInfo
import com.flixclusive.data.downloads.hls.ResolvedHlsPlaylist
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

internal class HlsManifestResolverImpl @Inject constructor(
    client: OkHttpClient,
    private val appDispatchers: AppDispatchers,
) : HlsManifestResolver {
    private val client by lazy {
        client
            .newBuilder()
            .cache(null)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    override suspend fun resolve(
        url: String,
        headers: Map<String, String>,
        mode: DownloadLinkSelectionMode,
    ): HlsResolutionResult =
        withContext(appDispatchers.io) {
            try {
                resolveInternal(url, headers, mode)
            } catch (e: Throwable) {
                HlsResolutionResult.Failed(e.message ?: "Failed to resolve HLS playlist")
            }
        }

    @OptIn(UnstableApi::class)
    private fun resolveInternal(
        url: String,
        headers: Map<String, String>,
        mode: DownloadLinkSelectionMode,
    ): HlsResolutionResult {
        val rootPlaylist = fetchAndParse(url, headers, HlsPlaylistParser())

        var mediaPlaylistUrl = url
        val mediaPlaylist = when (rootPlaylist) {
            is HlsMediaPlaylist -> rootPlaylist
            is HlsMultivariantPlaylist -> {
                val variant = selectVariant(rootPlaylist, mode)
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
        val segments = mediaPlaylist.segments.map { segment ->
            HlsSegmentInfo(
                url = UriUtil.resolve(mediaPlaylistUrl, segment.url),
                byteRangeOffset = segment.byteRangeOffset,
                byteRangeLength = segment.byteRangeLength,
                encryptionKeyUri = segment.fullSegmentEncryptionKeyUri?.let { UriUtil.resolve(mediaPlaylistUrl, it) },
                encryptionIv = segment.encryptionIV,
            )
        }

        return HlsResolutionResult.Success(ResolvedHlsPlaylist(segments))
    }

    /**
     * Mirrors CS3's `isPlayableStandalone`: a variant is safe to download standalone when it
     * either declares no separate audio group (so its own segments must contain the audio per the
     * HLS spec), or its audio group's rendition has no URI of its own (meaning that audio is
     * embedded in the variant's segments too) — and it isn't a trick-play (I-frame-only) variant.
     * There's no way to fully verify muxing without inspecting the TS segments themselves.
     *
     * Among those usable variants, [DownloadLinkSelectionMode.QUALITY_FIRST] picks the highest
     * resolution/bitrate and [DownloadLinkSelectionMode.SIZE_FIRST] the lowest — a variant's file
     * size tracks its resolution directly, so there's no independent size axis to probe here the
     * way there is for progressive links.
     */
    private fun selectVariant(
        playlist: HlsMultivariantPlaylist,
        mode: DownloadLinkSelectionMode,
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

        return when (mode) {
            DownloadLinkSelectionMode.QUALITY_FIRST -> usable.maxWithOrNull(bySize)
            DownloadLinkSelectionMode.SIZE_FIRST -> usable.minWithOrNull(bySize)
        }
    }

    private fun fetchAndParse(
        url: String,
        headers: Map<String, String>,
        parser: HlsPlaylistParser,
    ): HlsPlaylist {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (name, value) -> requestBuilder.addHeader(name, value) }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch HLS playlist: ${response.code}")
            return response.body.byteStream().use { stream -> parser.parse(Uri.parse(url), stream) }
        }
    }
}
