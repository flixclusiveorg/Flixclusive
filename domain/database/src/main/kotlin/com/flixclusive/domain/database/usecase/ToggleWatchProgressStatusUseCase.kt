package com.flixclusive.domain.database.usecase

import com.flixclusive.model.film.Film

/**
 * This use case allows adding or removing a movie or TV show episode from the watch progress.
 * */
interface ToggleWatchProgressStatusUseCase {
    /**
     * Toggles the watch progress status of a movie or TV show episode.
     *
     * @param film The film (movie or TV show episode) to toggle the watch progress status for.
     * */
    suspend operator fun invoke(film: Film)
}
