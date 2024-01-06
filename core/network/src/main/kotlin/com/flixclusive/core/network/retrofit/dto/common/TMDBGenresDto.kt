package com.flixclusive.core.network.retrofit.dto.common

import com.flixclusive.domain.model.tmdb.Genre

data class TMDBGenresDto(val genres: List<Genre>)


fun TMDBGenresDto.toList(): List<Genre> {
    return genres
}