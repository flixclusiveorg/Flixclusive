package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.common.domain.Async
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Season
import kotlinx.coroutines.flow.Flow

/**
 * Use case for fetching a specific season of a TV show
 * */
interface GetSeasonWithWatchProgressUseCase {
    /**
     * Fetches a specific season of a TV show
     *
     * @param show The TV show for which the season is to be fetched.
     * @param number The season number to fetch.
     *
     * @return A [Flow] emitting [Async] containing the fetched [Season] or an error.
     * */
    operator fun invoke(
        show: Show,
        number: Int,
    ): Flow<Async<SeasonWithProgress>>
}
