package com.flixclusive.provider

import android.content.Context
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.provider.dto.FilmInfo
import com.flixclusive.provider.dto.SearchResults
import com.flixclusive.provider.extractor.Extractor
import com.flixclusive.provider.util.FlixclusiveWebView
import com.flixclusive.provider.util.WebViewCallback
import okhttp3.OkHttpClient

/**
 * The base class for every provider api.
 * @param client The [OkHttpClient] instance used for network requests.
 */
abstract class ProviderApi(
    protected val client: OkHttpClient
) {
    /**
     * The name of the provider.
     */
    abstract val name: String

    /**
     * The base URL used for network requests. Defaults to an empty string.
     */
    open val baseUrl: String = ""

    /**
     * The list of supported extractors, embeds, or servers for this provider instance.
     */
    open val supportedExtractors: List<Extractor> = emptyList()

    /**
     * Whether this provider needs to use a WebView to scrape content
     * */
    open val useWebView: Boolean = false

    /**
     * Performs a search for films based on the provided query.
     * @param film The [Film] object of the film. It could either be a [Movie] or [TvShow].
     * @param page The page number for paginated results. Defaults to 1.
     * @return a [SearchResults] instance containing the search results.
     */
    open suspend fun search(
        film: Film,
        page: Int = 1,
    ): SearchResults {
        TODO("OPTIONAL: Not yet implemented")
    }

    /**
     * Retrieves detailed information about a film.
     * @param filmId The ID of the film. The ID must come from the [search] method.
     * @param filmType The type of film.
     * @return a [FilmInfo] instance containing the film's information.
     */
    open suspend fun getFilmInfo(
        filmId: String,
        filmType: FilmType,
    ): FilmInfo {
        TODO("OPTIONAL: Not yet implemented")
    }

    /**
     * Obtains source links for the provided film, season, and episode.
     * @param filmId The ID of the film. The ID must come from the [search] method.
     * @param film The [Film] object of the film. It could either be a [Movie] or [TvShow].
     * @param season The season number. Defaults to null if the film is a movie.
     * @param episode The episode number. Defaults to null if the film is a movie.
     * @param onLinkLoaded A callback function invoked when a [SourceLink] is loaded.
     * @param onSubtitleLoaded A callback function invoked when a [Subtitle] is loaded.
     */
    abstract suspend fun getSourceLinks(
        filmId: String,
        film: Film,
        season: Int? = null,
        episode: Int? = null,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    )

    open fun getWebView(
        context: Context,
        callback: WebViewCallback,
        film: Film,
        episode: TMDBEpisode? = null,
    ): FlixclusiveWebView? = null
}
