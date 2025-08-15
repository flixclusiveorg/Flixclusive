package com.flixclusive.data.tmdb.repository

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season

/**
 * Repository interface for TMDB metadata operations including movies, TV shows, seasons, and episodes.
 */
interface TMDBMetadataRepository {
    /**
     * Retrieves detailed metadata for a movie.
     *
     * @param id The TMDB movie ID
     * @return [Resource] containing [Movie] metadata or error
     */
    suspend fun getMovie(id: Int): Resource<Movie>

    /**
     * Retrieves detailed metadata for a TV show.
     *
     * @param id The TMDB TV show ID
     * @return [Resource] containing [TvShow] metadata or error
     */
    suspend fun getTvShow(id: Int): Resource<TvShow>

    /**
     * Retrieves metadata for a specific season of a TV show.
     *
     * @param id The TMDB TV show ID
     * @param seasonNumber The season number
     * @return [Resource] containing [Season] metadata or error
     */
    suspend fun getSeason(
        id: Int,
        seasonNumber: Int,
    ): Resource<Season>

    /**
     * Retrieves metadata for a specific episode.
     *
     * @param id The TMDB TV show ID
     * @param seasonNumber The season number
     * @param episodeNumber The episode number
     * @return [Resource] containing [Episode] metadata or null if not found
     */
    suspend fun getEpisode(
        id: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Resource<Episode?>
}
