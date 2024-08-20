package com.flixclusive.provider

import android.content.Context
import com.flixclusive.core.util.film.filter.Filter
import com.flixclusive.core.util.film.filter.FilterList
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.provider.ProviderCatalog
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.model.tmdb.SearchResponseData
import com.flixclusive.model.tmdb.TvShow
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.util.defaultTestFilm
import com.flixclusive.provider.webview.ProviderWebView
import com.flixclusive.provider.webview.ProviderWebViewCallback
import okhttp3.OkHttpClient

/**
 * The base class for every provider api.
 *
 * An api will provide source links for a given film. It could also be used to search for films and retrieve detailed information about them.
 *
 * @property client The [OkHttpClient] instance used for network requests.
 *
 * @property baseUrl The base URL used for network requests. Defaults to an empty string.
 * @property testFilm The [Film] to use for testing purposes. Defaults to [The Godfather (1972)](https://www.themoviedb.org/movie/238-the-godfather).
 * @property useWebView Whether this provider needs to use a WebView to scrape content. Defaults to false.
 * @property catalogs The list of [ProviderCatalog]s that this provider provides. Defaults to an empty list.
 * @property filters The list of [Filter]s that this provider's search method supports. Defaults to an empty list.
 */
@Suppress("unused")
abstract class ProviderApi(
    protected val client: OkHttpClient,
    val provider: Provider,
) {
    open val baseUrl: String = ""
    open val testFilm: FilmDetails = defaultTestFilm
    open val useWebView: Boolean = false
    open val filters: FilterList get() = FilterList()
    open val catalogs: List<ProviderCatalog> get() = emptyList()

    /**
     * Obtains a list of [Film] items from the provider's [catalogs].
     *
     * @param catalog The [ProviderCatalog] to load.
     * @param page The page number for paginated results. Defaults to 1.
     * @return A list of [FilmSearchItem] objects representing the films in the catalog.
     * By default, returns an empty list.
     */
    open suspend fun getCatalogItems(
        catalog: ProviderCatalog,
        page: Int = 1
    ): SearchResponseData<FilmSearchItem>
        = throw NotImplementedError()

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
    ): SearchResponseData<FilmSearchItem>
        = throw NotImplementedError()

    /**
     * Retrieves detailed information about a film.
     * @param film The [Film] object of the film to retrieve details for.
     * @return a [FilmDetails] instance containing the film's information. It could either be a [Movie] or [TvShow].
     */
    open suspend fun getFilmDetails(film: Film): FilmDetails
        = throw NotImplementedError()

    /**
     * Obtains resource links for the provided film, season, and episode.
     *
     * @param watchId The unique watch identifier for the film.
     * @param film The detailed film object of the film. Notice that it is a [FilmDetails] and not a [Film] object, this means that this film object has full details and not just the partial info of it. It could either be a [Movie] or [TvShow].
     * @param episode The [Episode] object of the episode. Defaults to null for movies.
     *
     * @return a list of [MediaLink] objects representing the links for the film.
     */
    open suspend fun getLinks(
        watchId: String,
        film: FilmDetails,
        episode: Episode? = null
    ): List<MediaLink>
        = throw NotImplementedError()

    open fun getWebView(
        context: Context,
        callback: ProviderWebViewCallback,
        film: FilmDetails,
        episode: Episode? = null,
    ): ProviderWebView
        = throw NotImplementedError("This provider indicates that it uses a WebView but does not provide one. Make it make sense!")
}
