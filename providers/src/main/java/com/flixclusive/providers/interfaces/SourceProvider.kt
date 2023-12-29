package com.flixclusive.providers.interfaces

import com.flixclusive.providers.models.common.MediaInfo
import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.SearchResults
import com.flixclusive.providers.models.common.SourceLink
import com.flixclusive.providers.models.common.Subtitle
import okhttp3.OkHttpClient

abstract class SourceProvider(protected val client: OkHttpClient) {
    abstract val name: String
    open val baseUrl: String = ""

    /**
     *
     * The embeds, extractors or servers of
     * this provider instance.
     *
     * */
    open val supportedExtractors: List<Extractor> = emptyList()

    abstract suspend fun search(query: String, page: Int = 1, mediaType: MediaType): SearchResults

    abstract suspend fun getMediaInfo(
        mediaId: String,
        mediaType: MediaType,
    ): MediaInfo

    /**
     *
     * Obtains source links for the film provided.
     *
     * */
    abstract suspend fun getSourceLinks(
        mediaId: String,
        season: Int? = null,
        episode: Int? = null,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    )
}