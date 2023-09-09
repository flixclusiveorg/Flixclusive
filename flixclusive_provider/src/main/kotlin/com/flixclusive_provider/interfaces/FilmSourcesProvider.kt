package com.flixclusive_provider.interfaces

import com.flixclusive_provider.models.common.MediaInfo
import com.flixclusive_provider.models.common.MediaType
import com.flixclusive_provider.models.common.SearchResults
import com.flixclusive_provider.models.common.VideoData
import com.flixclusive_provider.models.common.VideoDataServer

interface FilmSourcesProvider {
    val baseUrl: String
    fun search(query: String, page: Int = 1): SearchResults
    fun getMediaInfo(
        mediaId: String,
        mediaType: MediaType,
    ): MediaInfo
    fun getEpisodeId(
        mediaId: String,
        episode: Int,
        season: Int,
    ): String?
    fun getStreamingLinks(
        episodeId: String,
        mediaId: String,
        initialSourceUrl: String? = null,
        server: VideoDataServer? = null,
    ): VideoData
    fun getAvailableServers(
        episodeId: String,
        mediaId: String,
    ): List<VideoDataServer>
}