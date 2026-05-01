package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode

/**
 * Use case for fetching the next episode of a TV show based on the current season and episode numbers.
 * */
interface GetNextEpisodeUseCase {
    /**
     * Fetches the next episode of a TV show.
     *
     * @param show The [Show] object containing the seasons and episodes.
     * @param season The current season number.
     * @param episode The current episode number.
     *
     * @return The [Episode] if available, otherwise null.
     * */
    suspend operator fun invoke(
        show: Show,
        season: Int,
        episode: Int,
    ): Episode?
}
