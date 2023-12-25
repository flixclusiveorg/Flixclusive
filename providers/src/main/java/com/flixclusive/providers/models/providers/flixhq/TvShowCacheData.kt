package com.flixclusive.providers.models.providers.flixhq

import com.flixclusive.providers.models.common.MediaInfo
import org.jsoup.select.Elements


/**
 *
 * Used to cache data of previous called tv show.
 * This will ease rapid (tv) episodes requests from
 * the source/provider.
 *
 * */
data class TvShowCacheData(
    val id: String? = null,
    val mediaInfo: MediaInfo? = null,
    val seasons: Elements? = null,
    val episodes: Elements? = null,
)