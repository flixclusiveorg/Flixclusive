package com.flixclusive.domain.provider.usecase.get

import com.flixclusive.core.network.util.Resource
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Season

interface GetSeasonUseCase {
    suspend operator fun invoke(
        tvShow: TvShow,
        number: Int,
    ): Resource<Season?>
}
