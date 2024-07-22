package com.flixclusive.provider

import android.content.Context
import com.flixclusive.core.util.film.Filter
import com.flixclusive.core.util.film.FilterList
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.util.FlixclusiveWebView
import com.flixclusive.provider.util.WebViewCallback
import okhttp3.OkHttpClient

/**
 * The base class for every provider api.
 *
 * An api will provide source links for a given film. It could also be used to search for films and retrieve detailed information about them.
 *
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
     * Whether this provider needs to use a WebView to scrape content
     * */
    open val useWebView: Boolean = false

    /**
     *  The list of [Filter]s that this provider's search method supports.
     * */
    open val filters: FilterList
        get() = FilterList()

    /** This provider's own catalogs */
    open val catalogs: List<ProviderCatalog>
        get() = emptyList()

    /**
     * Obtains a list of [Film] items from the provider's catalog.
     *
     * @param catalog The [ProviderCatalog] to load.
     * @param page The page number for paginated results. Defaults to 1.
     * @return A list of [FilmSearchItem] objects representing the films in the catalog.
     * By default, returns an empty list.
     */
    open suspend fun getCatalogItems(
        catalog: ProviderCatalog,
        page: Int = 1
    ): SearchResponseData<FilmSearchItem> {
        TODO("OPTIONAL: Not yet implemented")
    }

    /**
     * Searches for films based on the provided criteria.
     *
     * @param title The title of the film to search for.
     * @param id The ID of the film to search for (optional).
     * @param tmdbId The TMDB ID of the film to search for (optional).
     * @param imdbId The IMDB ID of the film to search for (optional).
     * @param page The page number of the search results (optional, defaults to 1).
     * @param filters A list of filters to apply to the search (optional, defaults to an empty list).
     *
     * @return A [SearchResponseData] object containing the search results.
     */
    open suspend fun search(
        title: String,
        page: Int = 1,
        id: String? = null,
        imdbId: String? = null,
        tmdbId: Int? = null,
        filters: FilterList = this.filters,
    ): SearchResponseData<FilmSearchItem> {
        TODO("OPTIONAL: Not yet implemented")
    }

    /**
     * Retrieves detailed information about a film.
     * @param film The [Film] object of the film to retrieve details for.
     * @return a [FilmDetails] instance containing the film's information. It could either be a [Movie] or [TvShow].
     */
    open suspend fun getFilmDetails(film: Film): FilmDetails {
        TODO("OPTIONAL: Not yet implemented")
    }

    /**
     * Obtains source links for the provided film, season, and episode.
     * @param watchId The unique watch identifier for the film.
     * @param film The [Film] object of the film. It could either be a [Movie] or [TvShow].
     * @param episode The [Episode] object of the episode. Defaults to null for movies.
     * @param onLinkLoaded A callback function invoked when a [SourceLink] is loaded.
     * @param onSubtitleLoaded A callback function invoked when a [Subtitle] is loaded.
     */
    abstract suspend fun getSourceLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode? = null,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    )

    open fun getWebView(
        context: Context,
        callback: WebViewCallback,
        film: FilmDetails,
        episode: Episode? = null,
    ): FlixclusiveWebView? = null
}
