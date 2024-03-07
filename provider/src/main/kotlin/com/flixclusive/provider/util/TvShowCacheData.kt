package com.flixclusive.provider.util

import com.flixclusive.provider.dto.FilmInfo
import org.jsoup.select.Elements


/**
 * The cached data of previously called TV show.
 * This will ease rapid (TV) episodes requests from
 * the source/provider. This should be used for providers
 * that uses Jsoup [Elements] for their tv show seasons/episodes
 *
 * @param id The ID of the TV show. Defaults to null.
 * @param filmInfo Information about the TV show. Defaults to null.
 * @param seasons The seasons of the TV show. Defaults to null.
 * @param episodes The episodes of the TV show. Defaults to null.
 */
data class TvShowCacheData(
    val id: String? = null,
    val filmInfo: FilmInfo? = null,
    val seasons: Elements? = null,
    val episodes: Elements? = null,
)
