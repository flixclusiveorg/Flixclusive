package com.flixclusive.data.tmdb.repository

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.provider.link.Stream

/**
 * Repository interface for TMDB streaming provider operations.
 */
interface TMDBWatchProvidersRepository {
    /**
     * Retrieves available streaming providers for a movie or TV show.
     *
     * @param mediaType The type of media ("movie", "tv")
     * @param id The TMDB ID
     * @return [Resource] containing list of streaming providers or error
     */
    suspend fun getWatchProviders(
        mediaType: String,
        id: Int,
    ): Resource<List<Stream>>
}
