package com.flixclusive.data.tmdb.repository

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.film.TMDBCollection

/**
 * Repository interface for TMDB movie collection operations.
 */
interface TMDBMovieCollectionRepository {
    /**
     * Retrieves information about a movie collection.
     *
     * @param id The TMDB collection ID
     * @return [Resource] containing [TMDBCollection] data or error
     */
    suspend fun getCollection(id: Int): Resource<TMDBCollection>
}
