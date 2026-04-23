package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode
import kotlinx.coroutines.flow.Flow

/**
 * This use case is used to obtain the links of a film or episode
 * from a provider, which can be used to stream the film or episode.
 * */
interface GetMediaLinksUseCase {
    /**
     * Obtains the links of a film or episode using a cached watch ID.
     *
     * @param film The film or TV show to obtain the links for.
     * @param episode The episode of the TV show to obtain the links for, if applicable.
     * If the film is a movie, this can be null.
     *
     * @return A flow stream of [LoadLinksState]
     * */
    operator fun invoke(
        film: FilmMetadata,
        episode: Episode? = null,
    ): Flow<LoadLinksState>
}
