package com.flixclusive.provider.base

import com.flixclusive.core.util.film.FilmType
import com.flixclusive.extractor.base.Extractor
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.base.dto.FilmInfo
import com.flixclusive.provider.base.dto.SearchResults
import okhttp3.OkHttpClient

abstract class Provider(
    protected val client: OkHttpClient
) {
    abstract val name: String
    open val baseUrl: String = ""

    /**
     *
     * The embeds, extractors or servers of
     * this provider instance.
     *
     * */
    open val supportedExtractors: List<Extractor> = emptyList()

    abstract suspend fun search(
        query: String,
        page: Int = 1,
        filmType: FilmType,
    ): SearchResults

    abstract suspend fun getFilmInfo(
        filmId: String,
        filmType: FilmType,
    ): FilmInfo

    /**
     *
     * Obtains source links for the film provided.
     *
     * */
    abstract suspend fun getSourceLinks(
        filmId: String,
        season: Int? = null,
        episode: Int? = null,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    )
}