package com.flixclusive.data.library.custom

import com.flixclusive.core.network.retrofit.TMDBApiService
import com.flixclusive.core.network.util.Resource
import com.flixclusive.data.tmdb.SortOptions
import com.flixclusive.data.tmdb.TMDBRepository
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TMDBCollection
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.provider.link.Stream
import okhttp3.OkHttpClient

internal class FakeTMDBRepository(
    private val client: OkHttpClient,
    private val tmdbApiService: TMDBApiService
) : TMDBRepository {
    override suspend fun getMovie(id: Int): Resource<Movie> {
        throw NotImplementedError()
    }

    override suspend fun getTvShow(id: Int): Resource<TvShow> {
        throw NotImplementedError()
    }

    override suspend fun getSeason(id: Int, seasonNumber: Int): Resource<Season> {
        throw NotImplementedError()
    }

    override suspend fun getTrending(
        mediaType: String,
        timeWindow: String,
        page: Int
    ): Resource<SearchResponseData<FilmSearchItem>> {
        throw NotImplementedError()
    }

    override suspend fun discoverFilms(
        mediaType: String,
        page: Int,
        withNetworks: List<Int>?,
        withCompanies: List<Int>?,
        withGenres: List<Genre>?,
        sortBy: SortOptions
    ): Resource<SearchResponseData<FilmSearchItem>> {
        throw NotImplementedError()
    }

    override suspend fun search(query: String, page: Int, filter: Int): Resource<SearchResponseData<FilmSearchItem>> {
        throw NotImplementedError()
    }

    override suspend fun getLogo(mediaType: String, id: Int): Resource<String> {
        throw NotImplementedError()
    }

    override suspend fun getPosterWithoutLogo(mediaType: String, id: Int): Resource<String> {
        throw NotImplementedError()
    }

    override suspend fun getEpisode(id: Int, seasonNumber: Int, episodeNumber: Int): Resource<Episode?> {
        throw NotImplementedError()
    }

    override suspend fun getCollection(id: Int): Resource<TMDBCollection> {
        throw NotImplementedError()
    }

    override suspend fun paginateConfigItems(url: String, page: Int): Resource<SearchResponseData<FilmSearchItem>> {
        throw NotImplementedError()
    }

    override suspend fun getWatchProviders(
        mediaType: String,
        id: Int
    ): Resource<List<Stream>> {
        throw NotImplementedError()
    }
}
