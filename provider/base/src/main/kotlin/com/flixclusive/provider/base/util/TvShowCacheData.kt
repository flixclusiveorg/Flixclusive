package com.flixclusive.provider.base.util

import com.flixclusive.provider.base.dto.FilmInfo
import org.jsoup.select.Elements


/**
 *
 * The cached data of previous called tv show.
 * This will ease rapid (tv) episodes requests from
 * the source/provider.
 *
 * */
data class TvShowCacheData(
    val id: String? = null,
    val filmInfo: FilmInfo? = null,
    val seasons: Elements? = null,
    val episodes: Elements? = null,
)