package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import kotlinx.coroutines.flow.Flow

// TODO: Refactor to a repository instead of a use case
//  Then each `invoke` method should be renamed to `getLinks`
//  then the methods must return a flow of `MediaLink` instead of `LoadLinksState`.
//  We also need to merge the `CachedLinksRepository` into the `MediaLinksRepository`
//  The repository will handle caching internally.

/**
 * This use case is used to obtain the links of a film or episode
 * from a provider, which can be used to stream the film or episode.
 * */
interface GetMediaLinksUseCase {
    /**
     *
     * Obtains the links of the film and episode.
     *
     * @param movie The movie to obtain the links for.
     * @param providerId The ID of the provider to use to obtain the links.
     *
     * @return A flow stream of [LoadLinksState]
     * */
    operator fun invoke(
        movie: Movie,
        providerId: String? = null,
    ): Flow<LoadLinksState>

    /**
     * Obtains the links of the episode of a TV show.
     *
     * @param tvShow The TV show to obtain the links for.
     * @param episode The episode of the TV show to obtain the links for.
     * @param providerId The ID of the provider to use to obtain the links.
     *
     * @return A flow stream of [LoadLinksState]
     * */
    operator fun invoke(
        tvShow: TvShow,
        episode: Episode,
        providerId: String? = null,
    ): Flow<LoadLinksState>

    /**
     * Obtains the links of a film or episode using a cached watch ID.
     *
     * @param film The film or TV show to obtain the links for.
     * @param watchId The cached watch ID to use for the links.
     * @param episode The episode of the TV show to obtain the links for, if applicable.
     * If the film is a movie, this can be null.
     * @param providerId The ID of the provider to use to obtain the links.
     *
     * @return A flow stream of [LoadLinksState]
     * */
    operator fun invoke(
        film: FilmMetadata,
        watchId: String,
        episode: Episode? = null,
        providerId: String? = null,
    ): Flow<LoadLinksState>
}
