package com.flixclusive.core.network.util

import com.flixclusive.core.network.util.serializers.FilmSearchItemDeserializer
import com.flixclusive.core.network.util.serializers.PaginatedSearchItemsDeserializer
import com.flixclusive.core.network.util.serializers.TMDBMovieDeserializer
import com.flixclusive.core.network.util.serializers.TMDBTvShowDeserializer
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.SearchResponseData
import com.flixclusive.model.film.TvShow
import com.google.gson.Gson
import com.google.gson.GsonBuilder

internal fun getSearchItemGson(): Gson {
    return GsonBuilder()
        .registerTypeAdapter(FilmSearchItem::class.java, FilmSearchItemDeserializer())
        .registerTypeAdapter(Movie::class.java, TMDBMovieDeserializer())
        .registerTypeAdapter(TvShow::class.java, TMDBTvShowDeserializer())
        .registerTypeAdapter(SearchResponseData<FilmSearchItem>()::class.java, PaginatedSearchItemsDeserializer())
        .create()
}
