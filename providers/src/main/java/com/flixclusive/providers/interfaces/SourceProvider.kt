package com.flixclusive.providers.interfaces

import com.flixclusive.providers.models.common.MediaInfo
import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.SearchResults
import com.flixclusive.providers.models.common.VideoData
import okhttp3.OkHttpClient

internal typealias Server = String

abstract class SourceProvider(protected val client: OkHttpClient) {
    abstract val name: String
    open val baseUrl: String = ""

    /**
     *
     * The embeds, extractors or servers of
     * this provider instance.
     *
     * */
    open val supportedEmbeds: List<Server> = emptyList()

    abstract suspend fun search(query: String, page: Int = 1, mediaType: MediaType): SearchResults

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
}