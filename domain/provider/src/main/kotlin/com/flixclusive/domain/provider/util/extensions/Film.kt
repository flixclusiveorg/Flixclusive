package com.flixclusive.domain.provider.util.extensions

import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.Film

internal val Film.isNonDefaultProvider: Boolean
    get() = !providerId.equals(DEFAULT_FILM_SOURCE_NAME, true)
