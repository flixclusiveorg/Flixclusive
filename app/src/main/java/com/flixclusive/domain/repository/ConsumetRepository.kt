package com.flixclusive.domain.repository

import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.consumet.VideoData
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBEpisode

interface ConsumetRepository {
    val consumetDefaultWatchProvider: String
    val consumetDefaultVideoServer: String

    suspend fun getMovieStreamingLinks(
        consumetId: String,
        server: String
    ): Resource<VideoData?>
    suspend fun getTvShowStreamingLinks(
        consumetId: String,
        episode: TMDBEpisode,
        server: String
    ): Resource<VideoData?>
    suspend fun getConsumetFilmMediaId(film: Film?): String?
}