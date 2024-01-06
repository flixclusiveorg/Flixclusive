package com.flixclusive.provider.base.dto

data class FilmInfo(
    val id: String,
    val title: String,
    val yearReleased: String = "",
    val seasons: Int? = null,
    val episodes: Int? = null
)
