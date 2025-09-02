package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode

/**
 * Use case for fetching the next episode of a TV show based on the current season and episode numbers.
 * */
interface GetEpisodeUseCase {
    /**
     * Fetches the next episode of a TV show.
     *
     * @param tvShow The [TvShow] object containing the seasons and episodes.
     * @param season The current season number.
     * @param episode The current episode number.
     *
     * @return The [Episode] if available, otherwise null.
     * */
    suspend operator fun invoke(
        tvShow: TvShow,
        season: Int,
        episode: Int,
    ): Episode?
}
