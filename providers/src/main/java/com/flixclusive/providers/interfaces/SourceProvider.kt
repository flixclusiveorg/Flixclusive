package com.flixclusive.providers.interfaces

import com.flixclusive.providers.models.common.MediaInfo
import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.SearchResults
import com.flixclusive.providers.models.common.VideoData
import com.flixclusive.providers.models.common.VideoDataServer

internal typealias Server = String

abstract class SourceProvider() {
    abstract val name: String
    open val baseUrl: String = ""

    /**
     *
     * The embeds, extractors or servers of
     * this provider instance.
     *
     * */
    open val providerServers: List<Server> = emptyList()

    abstract suspend fun search(query: String, page: Int = 1): SearchResults

    abstract suspend fun getMediaInfo(
        mediaId: String,
        mediaType: MediaType,
    ): MediaInfo

    abstract suspend fun getSourceLinks(
        mediaId: String,
        server: String? = null,
        season: Int? = null,
        episode: Int? = null,
    ): VideoData

    open suspend fun getAvailableServers(
        mediaId: String,
        season: Int? = null,
        episode: Int? = null,
    ): List<VideoDataServer> = emptyList()
}