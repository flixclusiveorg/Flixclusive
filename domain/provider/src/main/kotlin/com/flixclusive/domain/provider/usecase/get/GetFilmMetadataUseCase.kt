package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata

/**
 * Use case for fetching detailed metadata of a film.
 * */
interface GetFilmMetadataUseCase {
    /**
     * Fetches detailed metadata for a given [film].
     *
     * The method first checks if the film's provider is not the default source. If it's a custom provider,
     * it attempts to fetch metadata using the corresponding provider API. If the provider is the default source, it will fetch from TMDB.
     *
     * @param film The film for which metadata is to be fetched.
     * @return A [Resource] containing [FilmMetadata] on success or an error message
     * */
    suspend operator fun invoke(film: Film): Resource<FilmMetadata>
}
