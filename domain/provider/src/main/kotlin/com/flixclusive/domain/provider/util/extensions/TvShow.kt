package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.TvShow

/**
 * Checks if the TvShow is from TMDB source by verifying if it has a non-null tmdbId
 * and if the providerId matches the default film source name (case-insensitive).
 *
 * TODO: Update core-stubs's [TvShow.isFromTmdb] so no need to redefine this here.
 * */
internal val TvShow.isFromTmdbSource: Boolean
    get() = tmdbId != null && providerId.equals(DEFAULT_FILM_SOURCE_NAME, ignoreCase = true)
