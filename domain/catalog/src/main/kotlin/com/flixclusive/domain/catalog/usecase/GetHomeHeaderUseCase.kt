package com.flixclusive.domain.catalog.usecase

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.film.Film

/**
 * Use case to retrieve the header item for the home screen.
 *
 * This use case is responsible for fetching the featured film or series that is displayed at the top of the home screen.
 */
interface GetHomeHeaderUseCase {
    /**
     * Fetches the header item for the home screen.
     *
     * This function retrieves the header item that is displayed at the top of the home screen.
     * The header item typically includes a featured film or series that is highlighted for users.
     *
     * @return A [Resource] containing the header item, which is a [Film] object, or an error if the fetch fails.
     */
    suspend operator fun invoke(): Resource<Film>
}
