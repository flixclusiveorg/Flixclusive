package com.flixclusive.provider.base

import com.flixclusive.core.util.film.FilmType
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.provider.base.dto.FilmInfo
import com.flixclusive.provider.base.dto.SearchResults
import com.flixclusive.provider.base.extractor.Extractor
import okhttp3.OkHttpClient

/**
 * The base class for every provider.
 * @param client The [OkHttpClient] instance used for network requests.
 */
abstract class Provider(
    protected val client: OkHttpClient
) {
    /**
     * The name of the provider.
     */
    abstract val name: String

    /**
     * The base URL used for network requests. Defaults to an empty string.
     */
    protected open val baseUrl: String = ""

    /**
     * The list of supported extractors, embeds, or servers for this provider instance.
     */
    protected open val supportedExtractors: List<Extractor> = emptyList()

    /**
     * Performs a search for films based on the provided query.
     * @param query The search query.
     * @param page The page number for paginated results. Defaults to 1.
     * @param filmType The type of film being searched for.
     * @return a [SearchResults] instance containing the search results.
     */
    abstract suspend fun search(
        query: String,
        page: Int = 1,
        filmType: FilmType,
    ): SearchResults

    /**
     * Retrieves detailed information about a film.
     * @param filmId The ID of the film.
     * @param filmType The type of film.
     * @return a [FilmInfo] instance containing the film's information.
     */
    abstract suspend fun getFilmInfo(
        filmId: String,
        filmType: FilmType,
    ): FilmInfo

    /**
     * Obtains source links for the provided film, season, and episode.
     * @param filmId The ID of the film.
     * @param season The season number. Defaults to null if the film is a movie.
     * @param episode The episode number. Defaults to null if the film is a movie.
     * @param onLinkLoaded A callback function invoked when a [SourceLink] is loaded.
     * @param onSubtitleLoaded A callback function invoked when a [Subtitle] is loaded.
     */
    abstract suspend fun getSourceLinks(
        filmId: String,
        season: Int? = null,
        episode: Int? = null,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    )
}
