package com.flixclusive.domain.database.usecase

import com.flixclusive.model.film.Film

/**
 * This use case allows adding or removing a movie or TV show episode from the watchlist.
 * */
interface ToggleWatchlistStatusUseCase {
    /**
     * Toggles the watchlist status of a movie or TV show episode.
     *
     * @param film The [Film] associated with the watchlist item, if applicable.
     * */
    suspend operator fun invoke(film: Film)
}
