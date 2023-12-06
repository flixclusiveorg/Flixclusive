package com.flixclusive.providers.models.providers.flixhq

import com.flixclusive.providers.models.common.MediaInfo
import org.jsoup.select.Elements

data class TvShowCacheData(
    val id: String? = null,
    val mediaInfo: MediaInfo? = null,
    val seasons: Elements? = null,
    val episodes: Elements? = null,
)