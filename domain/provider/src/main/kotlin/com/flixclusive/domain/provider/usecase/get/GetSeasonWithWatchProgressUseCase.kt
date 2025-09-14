package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.network.util.Resource
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Season
import kotlinx.coroutines.flow.Flow

/**
 * Use case for fetching a specific season of a TV show from TMDB.
 * */
interface GetSeasonWithWatchProgressUseCase {
    /**
     * Fetches a specific season of a TV show from TMDB.
     *
     * @param tvShow The TV show for which the season is to be fetched.
     * @param number The season number to fetch.
     *
     * @return A [Flow] emitting [Resource] containing the fetched [Season] or an error.
     * */
    operator fun invoke(
        tvShow: TvShow,
        number: Int,
    ): Flow<Resource<SeasonWithProgress>>
}
