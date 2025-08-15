package com.flixclusive.data.tmdb.repository

import com.flixclusive.core.network.util.Resource

/**
 * Repository interface for TMDB asset operations including images and logos.
 */
interface TMDBAssetsRepository {
    /**
     * Retrieves the logo image URL for a movie or TV show.
     *
     * @param mediaType The type of media ("movie", "tv")
     * @param id The TMDB ID
     * @return [Resource] containing the logo image URL or error
     */
    suspend fun getLogo(
        mediaType: String,
        id: Int,
    ): Resource<String>

    /**
     * Retrieves a poster image URL without logo overlay.
     *
     * @param mediaType The type of media ("movie", "tv")
     * @param id The TMDB ID
     * @return [Resource] containing the poster image URL or error
     */
    suspend fun getPosterWithoutLogo(
        mediaType: String,
        id: Int,
    ): Resource<String>
}
